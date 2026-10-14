package com.rex.api.internal.skeleton.handler

import com.fasterxml.jackson.annotation.JsonProperty
import com.rex.api.internal.skeleton.enums.ErrorCode
import com.rex.api.internal.skeleton.exception.ApiCallException
import com.rex.api.internal.skeleton.exception.ClientBadRequestException
import com.rex.api.internal.skeleton.exception.ErrorMessageService
import com.rex.api.internal.skeleton.exception.ItemNotFoundException
import com.rex.api.internal.skeleton.model.ErrorResponseModel
import com.rex.api.internal.skeleton.util.logger
import jakarta.validation.ConstraintViolationException
import org.springframework.boot.json.JsonParseException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import tools.jackson.core.JacksonException
import java.io.IOException

@RestControllerAdvice
class GlobalExceptionHandler(
    private val errorMessageService: ErrorMessageService,
) {
    // ================================
    // Custom Exception Handlers
    // ================================

    @ExceptionHandler(ItemNotFoundException::class)
    protected fun handleItemNotFoundException(ex: ItemNotFoundException): ResponseEntity<ErrorResponseModel> {
        logger.error("ItemNotFoundException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_FOUND_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_NOT_FOUND)
    }

    @ExceptionHandler(ClientBadRequestException::class)
    protected fun handleClientBadRequestException(ex: ClientBadRequestException): ResponseEntity<ErrorResponseModel> {
        logger.error("ClientBadRequestException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.BAD_REQUEST_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    @ExceptionHandler(ApiCallException::class)
    protected fun handleApiCallException(ex: ApiCallException): ResponseEntity<ErrorResponseModel> {
        logger.error("ApiCallException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.INTERNAL_SERVER_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HttpStatus.valueOf(ex.httpStatus.value()))
    }

    // ================================
    // Validation Exception Handlers
    // ================================

    // API 호출 시 '객체' 혹은 '파라미터' 데이터 값이 유효하지 않은 경우
    @ExceptionHandler(ConstraintViolationException::class)
    protected fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleConstraintViolationException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_VALID_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // API 호출 시 '객체' 혹은 '파라미터' 데이터 값이 유효하지 않은 경우
    // @JsonProperty 어노테이션이 있는 경우 해당 이름으로 필드명 변환
    @ExceptionHandler(MethodArgumentNotValidException::class)
    protected fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleMethodArgumentNotValidException", ex)

        val targetClass: Class<*>? = ex.bindingResult.target?.javaClass
        val fieldToJsonKeyMap = targetClass?.let { getJsonPropertyMap(it) } ?: emptyMap()

        val errorMessage =
            ex.bindingResult.fieldErrors
                .map { error ->
                    val field = fieldToJsonKeyMap.getOrDefault(error.field, error.field)
                    val message = error.defaultMessage ?: "유효하지 않은 값입니다"
                    "$field: $message"
                }.distinct()
                .joinToString(", ")

        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_VALID_ERROR, errorMessage)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // API 호출 시 '객체' 혹은 '파라미터' 데이터 값이 유효하지 않은 경우
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    protected fun handleMethodArgumentTypeMismatchException(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleMethodArgumentTypeMismatchException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_VALID_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // API 호출 시 'Header' 내에 데이터 값이 유효하지 않은 경우
    @ExceptionHandler(MissingRequestHeaderException::class)
    protected fun handleMissingRequestHeaderException(ex: MissingRequestHeaderException): ResponseEntity<ErrorResponseModel> {
        logger.error("MissingRequestHeaderException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.REQUEST_BODY_MISSING_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // 요청 Body 를 읽지 못한 경우.
    //
    // 「본문 누락」과 「본문 형식 오류」를 구분한다. 둘 다 400 이지만 소비자가 재시도 여부를
    // 판단하려면 달라야 한다 — 누락은 본문을 안 보낸 것이고, 형식 오류는 보냈는데 깨진 것이다.
    // 원인이 Jackson 파싱 실패면 JACKSON_PROCESS_ERROR, 그 외(본문 부재 등)는 기존대로.
    @ExceptionHandler(HttpMessageNotReadableException::class)
    protected fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponseModel> {
        logger.error("HttpMessageNotReadableException", ex)
        val errorCode =
            if (hasJacksonCause(ex)) {
                ErrorCode.JACKSON_PROCESS_ERROR
            } else {
                ErrorCode.REQUEST_BODY_MISSING_ERROR
            }
        val response: ErrorResponseModel = ErrorResponseModel.of(errorCode, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // Spring MVC 는 본문 파싱 실패를 HttpMessageNotReadableException 으로 감싸므로
    // 원인 사슬을 따라가야 Jackson 예외인지 알 수 있다.
    private fun hasJacksonCause(ex: Throwable): Boolean {
        var cause: Throwable? = ex.cause
        while (cause != null) {
            if (cause is JacksonException) return true
            cause = cause.cause
        }
        return false
    }

    // 클라이언트에서 request로 '파라미터로' 데이터가 넘어오지 않았을 경우
    @ExceptionHandler(MissingServletRequestParameterException::class)
    protected fun handleMissingRequestHeaderExceptionException(ex: MissingServletRequestParameterException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleMissingServletRequestParameterException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.MISSING_REQUEST_PARAMETER_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // ================================
    // HTTP Error Handlers
    // ================================

    // 잘못된 서버 요청일 경우 발생한 경우
    @ExceptionHandler(BadRequest::class)
    protected fun handleBadRequestException(ex: HttpClientErrorException): ResponseEntity<ErrorResponseModel> {
        logger.error("HttpClientErrorException.BadRequest", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.BAD_REQUEST_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // 잘못된 주소로 요청 한 경우
    @ExceptionHandler(NoHandlerFoundException::class)
    protected fun handleNoHandlerFoundExceptionException(ex: NoHandlerFoundException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleNoHandlerFoundExceptionException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_FOUND_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_NOT_FOUND)
    }

    // 잘못된 주소로 요청 한 경우
    @ExceptionHandler(NoResourceFoundException::class)
    protected fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleNoHandlerFoundExceptionException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_FOUND_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_NOT_FOUND)
    }

    // ================================
    // General Exception Handlers
    // ================================

    // NULL 값이 발생한 경우
    @ExceptionHandler(NullPointerException::class)
    protected fun handleNullPointerException(ex: NullPointerException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleNullPointerException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NULL_POINT_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(NoSuchElementException::class)
    protected fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<ErrorResponseModel> {
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_FOUND_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_NOT_FOUND)
    }

    @ExceptionHandler(IOException::class)
    protected fun handleIOException(ex: IOException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleIOException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.IO_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(JsonParseException::class)
    protected fun handleJsonParseExceptionException(ex: JsonParseException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleJsonParseExceptionException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.JSON_PARSE_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    // tools.jackson.core 내에 Exception 발생하는 경우
    @ExceptionHandler(JacksonException::class)
    protected fun handleJacksonException(ex: JacksonException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleJacksonException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.REQUEST_BODY_MISSING_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    protected fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponseModel> {
        logger.error("handleIllegalArgumentException", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.NOT_VALID_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    protected fun handleAllExceptions(ex: Exception): ResponseEntity<ErrorResponseModel> {
        logger.error("Exception", ex)
        val response: ErrorResponseModel = ErrorResponseModel.of(ErrorCode.INTERNAL_SERVER_ERROR, ex.message)
        return ResponseEntity<ErrorResponseModel>(response, HTTP_STATUS_INTERNAL_SERVER_ERROR)
    }

    // ================================
    // Helper Methods
    // ================================

    /**
     * 클래스의 필드에서 @JsonProperty 어노테이션의 값을 추출하여 필드명과 매핑
     * @param clazz 대상 클래스
     * @return 필드명 -> JsonProperty 값 매핑
     */
    private fun getJsonPropertyMap(clazz: Class<*>): Map<String, String> =
        clazz.declaredFields.associate { field ->
            val jsonProperty = field.getAnnotation(JsonProperty::class.java)
            field.name to (jsonProperty?.value ?: field.name)
        }

    companion object {
        private val logger = logger()

        private val HTTP_STATUS_BAD_REQUEST = HttpStatus.BAD_REQUEST
        private val HTTP_STATUS_NOT_FOUND = HttpStatus.NOT_FOUND
        private val HTTP_STATUS_INTERNAL_SERVER_ERROR = HttpStatus.INTERNAL_SERVER_ERROR
    }
}