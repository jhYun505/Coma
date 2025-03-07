package com.coma.coma.sms;

import com.coma.coma.users.service.UserService;
import lombok.RequiredArgsConstructor;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;


@RestController
@RequestMapping("/api/sms")
public class SmsController {

    private final DefaultMessageService messageService;
    private UserService userService;

    // 인증번호를 임시로 저장할 맵
    private final Map<String, String> verificationCodes = new ConcurrentHashMap<>();

    public SmsController() {
        // 반드시 계정 내 등록된 유효한 API 키, API Secret Key를 입력해야함
        this.messageService = NurigoApp.INSTANCE.initialize("NCSGFBKWMHFWP0IR", "NEAZDMGEUQHRCS8VVXW6QNVAA1BR3DIU", "https://api.coolsms.co.kr");
    }

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/send")
    public SingleMessageSentResponse sendOne(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");

        Message message = new Message();
        // 발신번호 및 수신번호는 반드시 01012345678 형태로 입력되어야 합니다.
        message.setFrom("01076407715"); //발신번호
        message.setTo(phoneNumber); // 수신번호
        String verificationCode = generateRandomNumber();
        message.setText("[COMA] 아래의 인증번호를 입력해주세요\n" + verificationCode);

        try {
            // 인증번호를 메모리에 저장
            verificationCodes.put(phoneNumber, verificationCode);

            SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
            System.out.println(response);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("SMS 전송에 실패했습니다.");
        }
    }

    // 비밀번호 찾기 시 사용할 SMS 전송 API
    @PostMapping("/find-password/send")
    public ResponseEntity<String> sendResetPasswordVerificationCode(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        String phoneNumber = body.get("phoneNumber");

        // 사용자 존재 여부 확인
        if (!userService.checkUserExists(id, phoneNumber)) {
            return ResponseEntity.status(400).body("해당 아이디와 전화번호를 가진 사용자가 존재하지 않습니다.");
        }

        Message message = new Message();
        message.setFrom("01076407715"); // 발신번호
        message.setTo(phoneNumber); // 수신번호
        String verificationCode = generateRandomNumber();
        message.setText("[COMA] 아래의 인증번호를 입력해주세요\n" + verificationCode);

        try {
            verificationCodes.put(phoneNumber, verificationCode);
            SingleMessageSentResponse response = this.messageService.sendOne(new SingleMessageSendingRequest(message));
            return ResponseEntity.ok("인증번호가 전송되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("SMS 전송에 실패했습니다.");
        }
    }


    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestBody Map<String, String> body) {
        String phoneNumber = body.get("phoneNumber");
        String inputCode = body.get("code");

        // 서버에 저장된 인증번호 가져오기
        String storedCode = verificationCodes.get(phoneNumber);

        if (storedCode != null && storedCode.equals(inputCode)) {
            // 인증 성공
            verificationCodes.remove(phoneNumber); // 인증 완료 후 인증번호 제거
            return ResponseEntity.ok("인증이 완료되었습니다.");
        } else {
            // 인증 실패
            return ResponseEntity.status(400).body("인증에 실패했습니다. 인증번호를 다시 확인해주세요.");
        }
    }

    private String generateRandomNumber() {
        Random rand = new Random();
        StringBuilder numStr = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            numStr.append(rand.nextInt(10));
        }
        return numStr.toString();
    }
}
