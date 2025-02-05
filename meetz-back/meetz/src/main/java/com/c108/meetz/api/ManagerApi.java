package com.c108.meetz.api;

import com.c108.meetz.dto.ApiResponse;
import com.c108.meetz.dto.request.JoinRequestDto;
import com.c108.meetz.exception.BadRequestException;
import com.c108.meetz.service.MailService;
import com.c108.meetz.service.ManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class ManagerApi {

    private final ManagerService managerService;
    private final MailService mailService;


    @PostMapping("/join")
    public ApiResponse<Void> joinManager(@Valid @RequestBody JoinRequestDto joinRequestDto, BindingResult bindingResult) {
        if(bindingResult.hasErrors()){
            throw new BadRequestException("올바른 형식이 아닙니다.");
        }
        managerService.joinManager(joinRequestDto);
        return ApiResponse.success(OK);
    }

    //이메일 중복
    @GetMapping("/checkemail")
    public ApiResponse<Void> checkEmail(@RequestParam(value="email") String email) {

        if (mailService.checkEmail(email)) {
            throw new BadRequestException("이미 가입된 이메일입니다.");
        }
        return ApiResponse.success(OK);
    }

    //이메일 인증 요청
    @GetMapping("/authemail")
    public ApiResponse<Void> authEmail(@RequestParam(value="email") String email) {

        //메일 보내고 UUID 받기
        int sendedNum = mailService.sendMail(email);
        log.info("email={}  ", email);
        //redis에 <email, sendedNum> 넣기
        mailService.saveEmail(email, Integer.toString(sendedNum));
        return ApiResponse.success(OK);
    }

    //이메일 인증 번호 일치 확인
    @GetMapping("/checkauth")
    public ApiResponse<Void> getEmail(@RequestParam(value="email") String email, @RequestParam(value="authcode") String authcode) {

        String redisEmail = mailService.getEmail(authcode);

        if (redisEmail != null && redisEmail.equals(email)) {
            mailService.delEmail(authcode);
            return ApiResponse.success(OK);
        }
        throw new BadRequestException("인증번호가 일치하지 않습니다.");
    }

    @GetMapping("/test")
    public ApiResponse<Void> testFun() {
        return ApiResponse.success(OK);
    }
}
