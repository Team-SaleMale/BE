package com.salemale.domain.test;

import com.salemale.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@Tag(name = "테스트 API", description = "시스템 테스트용 API")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/data")
    @Operation(summary = "성공 데이터 조회", description = "정상 응답 테스트용 API")
    public ResponseEntity<ApiResponse<String>> success() {
        String data = testService.getSuccessData();
        return ResponseEntity.ok(ApiResponse.onSuccess(data));
    }

    @GetMapping("/error")
    @Operation(summary = "에러 테스트", description = "예외 처리 테스트용 API")
    public void error() {
        testService.throwError();
    }
}