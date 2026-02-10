package com.web3.community.common.exception

class BusinessException(val errorCode: ErrorCode) : RuntimeException(errorCode.message)
