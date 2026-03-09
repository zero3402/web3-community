1. Web3 게시판 만들기
- MSA
  - Vue.js
  - MySQL
  - MongoDB
  - Redis
  - Kafka
  - Webflux
  - MVC
  - Kotlin
  - Kubenetes

- 기능
  - 로그인
    - 일반 로그인, Google/Naver/Kakao 로그인
    - 게시판 조회/작성/수정/삭제, 좋아요/싫어요
    - 블록체인 지갑 생성(BTC, ETH(ERC20 토큰 포함))
      - 키는 암호화해서 관리
    - 블록체인 지갑 전송(BTC, ETH(ERC20 토큰 포함))
      - 수수료 최적화해서 전송
      - 다중 출금

 - 아키텍처 요구사항
   - API Gateway, Auth, User, Blockchain, Board 서버가 각각 다른 프로젝트
     - 공통 모듈이 있으면 공통 모듈화
   - 블록체인 다중 출금으로 인한 Utxo, Nonce 관리 문제 해결 등과 같은 대용량 처리

 - 기타
   - CLAUDE 컨택스트 관리를 위해 CLAUDE.md에 컨텍스트 실시간 저장
   - .gitignore 작성
   - 주석 작성
     - 모든 코드에 대한 주석 작성
   - MD 파일 작성
     - 아키텍처 작성
     - 각 서버마다 기술 선정 이유 작성


