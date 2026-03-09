package com.web3community.gateway.filter

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 요청/응답 로깅 게이트웨이 필터
 *
 * ## 주요 기능
 * 1. **X-Request-ID 주입**: 요청마다 고유한 추적 ID를 부여한다.
 *    - 클라이언트가 헤더를 제공한 경우 그 값을 사용 (분산 추적 연계)
 *    - 없는 경우 UUID v4를 자동 생성
 * 2. **요청 로깅**: HTTP 메서드, 경로, 클라이언트 IP, X-Request-ID를 기록
 * 3. **응답 로깅**: HTTP 상태 코드, 처리 시간(ms)을 기록
 * 4. **MDC 설정**: SLF4J MDC(Mapped Diagnostic Context)에 requestId를 추가하여
 *    해당 요청의 모든 로그에 자동으로 requestId가 포함되도록 한다.
 *
 * ## 로그 포맷 예시
 * ```
 * [req-start]  GET /api/v1/boards/1 | requestId=abc123 | ip=192.168.1.1
 * [req-end]    GET /api/v1/boards/1 | requestId=abc123 | status=200 | elapsed=45ms
 * ```
 *
 * ## Webflux 주의사항
 * Webflux는 요청과 응답이 다른 스레드에서 처리될 수 있으므로
 * ThreadLocal 기반의 MDC는 체인 종료 후 반드시 정리(clear)해야 한다.
 * 여기서는 응답 콜백(doFinally)에서 MDC를 정리한다.
 */
@Component
class LoggingFilter : AbstractGatewayFilterFactory<LoggingFilter.Config>(Config::class.java) {

    private val log = LoggerFactory.getLogger(javaClass)

    /** 요청 추적 ID 헤더 이름 */
    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"

        /** MDC에 저장할 키 이름 (application.yml의 로그 패턴에서 %X{requestId}로 참조) */
        const val MDC_REQUEST_ID_KEY = "requestId"
    }

    /**
     * 로깅 필터 설정 클래스
     *
     * 향후 확장: 요청/응답 바디 로깅 여부, 민감 헤더 마스킹 설정 등
     */
    class Config {
        // 현재는 설정 없음. 향후 logBody: Boolean = false 등 추가 가능
    }

    /**
     * 로깅 필터를 생성하고 반환한다.
     *
     * 요청 시작 시 ID를 주입하고, 응답 완료 시 경과 시간을 기록하는
     * Reactor 파이프라인을 구성한다.
     *
     * @param config 필터 설정
     * @return 로깅이 적용된 [GatewayFilter]
     */
    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->

            // ── X-Request-ID 처리 ──────────────────────────────────────────────
            // 클라이언트가 이미 요청 ID를 제공한 경우 그 값을 사용 (분산 추적 연계)
            // 없는 경우 서버에서 UUID를 생성하여 부여
            val requestId = exchange.request.headers.getFirst(REQUEST_ID_HEADER)
                ?: UUID.randomUUID().toString().replace("-", "").substring(0, 16)

            // MDC에 requestId 설정: 이후 해당 요청 처리 중 발생하는 모든 로그에 자동 포함
            MDC.put(MDC_REQUEST_ID_KEY, requestId)

            // ── 요청 헤더에 X-Request-ID 추가 ─────────────────────────────────
            // downstream 서비스도 동일한 requestId를 로그에 기록할 수 있도록 전달
            val mutatedRequest: ServerHttpRequest = exchange.request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build()

            val mutatedExchange: ServerWebExchange = exchange.mutate()
                .request(mutatedRequest)
                .build()

            // 요청 시작 시각 기록 (나노초: 높은 정밀도의 경과 시간 계산)
            val startTime = System.nanoTime()

            // ── 요청 로그 출력 ─────────────────────────────────────────────────
            logRequest(mutatedExchange.request, requestId)

            // ── 필터 체인 실행 및 응답 로그 ────────────────────────────────────
            chain.filter(mutatedExchange)
                // then(): 응답 완료(정상/에러 모두) 후 응답 로그 기록
                .then(
                    Mono.fromRunnable<Void> {
                        logResponse(mutatedExchange, requestId, startTime)
                        // 응답 헤더에도 X-Request-ID 추가 (클라이언트가 추적 ID를 확인할 수 있도록)
                        mutatedExchange.response.headers.set(REQUEST_ID_HEADER, requestId)
                    }
                )
                // doFinally: 스트림 종료 시(완료, 에러, 취소 모두) MDC 정리
                // Webflux에서 ThreadLocal은 요청 완료 후 반드시 정리해야 메모리 누수를 막는다
                .doFinally { MDC.remove(MDC_REQUEST_ID_KEY) }
        }
    }

    /**
     * 요청 정보를 로그로 기록한다.
     *
     * @param request 서버 HTTP 요청 객체
     * @param requestId 요청 추적 ID
     */
    private fun logRequest(request: ServerHttpRequest, requestId: String) {
        val method = request.method.name()
        val path = request.uri.path
        val query = request.uri.query?.let { "?$it" } ?: ""
        // X-Forwarded-For: 프록시/로드밸런서 뒤에 있을 때 실제 클라이언트 IP
        val clientIp = request.headers.getFirst("X-Forwarded-For")
            ?: request.remoteAddress?.address?.hostAddress
            ?: "unknown"

        log.info(
            "[req-start] {} {}{} | requestId={} | ip={}",
            method, path, query, requestId, clientIp
        )
    }

    /**
     * 응답 정보와 처리 시간을 로그로 기록한다.
     *
     * @param exchange 서버 웹 교환 컨텍스트 (요청 및 응답 포함)
     * @param requestId 요청 추적 ID
     * @param startTimeNano 요청 시작 시각 (나노초)
     */
    private fun logResponse(
        exchange: ServerWebExchange,
        requestId: String,
        startTimeNano: Long,
    ) {
        val response: ServerHttpResponse = exchange.response
        val request: ServerHttpRequest = exchange.request

        val method = request.method.name()
        val path = request.uri.path
        val statusCode = response.statusCode?.value() ?: 0

        // 나노초 → 밀리초 변환
        val elapsedMs = (System.nanoTime() - startTimeNano) / 1_000_000

        // 상태 코드에 따라 로그 레벨 분리
        // 5xx: ERROR, 4xx: WARN, 그 외: INFO
        when {
            statusCode >= 500 -> log.error(
                "[req-end] {} {} | requestId={} | status={} | elapsed={}ms",
                method, path, requestId, statusCode, elapsedMs
            )
            statusCode >= 400 -> log.warn(
                "[req-end] {} {} | requestId={} | status={} | elapsed={}ms",
                method, path, requestId, statusCode, elapsedMs
            )
            else -> log.info(
                "[req-end] {} {} | requestId={} | status={} | elapsed={}ms",
                method, path, requestId, statusCode, elapsedMs
            )
        }
    }
}
