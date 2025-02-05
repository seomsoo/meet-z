package com.c108.meetz.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.c108.meetz.domain.Manager;
import com.c108.meetz.domain.Meeting;
import com.c108.meetz.domain.Report;
import com.c108.meetz.domain.User;
import com.c108.meetz.dto.response.ReportDetailResponseDto;
import com.c108.meetz.dto.response.ReportListResponseDto;
import com.c108.meetz.dto.response.ReportsListResponseDto;
import com.c108.meetz.dto.response.TranscriptionResponseDto;
import com.c108.meetz.exception.BadRequestException;
import com.c108.meetz.exception.DuplicateException;
import com.c108.meetz.exception.NotFoundException;
import com.c108.meetz.repository.ManagerRepository;
import com.c108.meetz.repository.MeetingRepository;
import com.c108.meetz.repository.ReportRepository;
import com.c108.meetz.repository.UserRepository;
import com.c108.meetz.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static com.c108.meetz.domain.Role.FAN;
import static com.c108.meetz.domain.Role.STAR;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final AudioProcessingService audioProcessingService;
    private final AmazonS3 s3Client;
    private final ManagerRepository managerRepository;

    /**
     * 현재 스타 사용자가 팬(Fan)을 신고하는 메서드.
     */
    public void saveReport(int userId) {
        User star = getCurrentUser(); // 현재 로그인된 스타 사용자
        User fan = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("팬을 찾을 수 없습니다."));

        if (fan.getRole() != FAN) {
            throw new BadRequestException("신고 대상이 팬이 아닙니다.");
        }

        Meeting meeting = fan.getMeeting();  // 팬이 속한 미팅을 가져옴

        // 이미 신고된 사항인지 확인
        boolean reportExists = reportRepository.existsByStar_UserIdAndFan_UserId(star.getUserId(), fan.getUserId());

        if (reportExists) {
            throw new DuplicateException("이미 신고하였습니다.");
        }

        Report report = new Report(meeting, star, fan, true, false, null);
        reportRepository.save(report);
    }

    /**
     * 현재 스타 사용자가 신고를 취소하는 메서드.
     */
    public void cancelReport(int userId) {
        User star = getCurrentUser(); // 현재 로그인된 스타 사용자
        User fan = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("팬을 찾을 수 없습니다."));

        Meeting meeting = fan.getMeeting();  // 팬이 속한 미팅을 가져옴

        // 리포트를 조회하고 null 체크로 예외 처리
        Report report = reportRepository.findByStar_UserIdAndFan_UserId(star.getUserId(), fan.getUserId());

        if (report == null) {
            throw new NotFoundException("신고를 찾을 수 없습니다.");
        }

        // 리포트가 존재하면 삭제 작업 수행
        reportRepository.delete(report);
    }
    /**
     * 매니저가 특정 미팅에 대한 신고 목록을 조회하는 메서드.
     */
    public ReportsListResponseDto getReportList(int meetingId) {
        // 미팅 id대한 report List, 디테일
        // report meeting 찾고 거기에 있는 meeting.getManager()
        // 1. 지금 접속한 매니저
        // 2. 미팅레포지터리에서 meetingId로 meeting을 찾음
        // 3. meeting.getmanager = 지금 접속한 매니저
        Manager currentManager = getManager();

        // 미팅 정보 조회
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        // 미팅의 매니저와 현재 로그인된 매니저가 일치하는지 확인
        if (!meeting.getManager().equals(currentManager)) {
            throw new BadRequestException("접근 권한이 없습니다.");
        }

        // 참가자 수 계산
        int participantCount = meetingRepository.countFansInMeeting(meetingId);

        // 신고 목록 조회
        List<Report> reports = reportRepository.findByMeeting_MeetingId(meetingId);

        // ReportListResponseDto 생성 시 참가자 수를 포함
        return new ReportsListResponseDto(meeting, reports, participantCount);
    }

    /**
     * 매니저가 특정 신고에 대한 세부 정보를 조회하는 메서드.
     */
    public ReportDetailResponseDto getReportDetail(int meetingId, int reportId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new NotFoundException("Meeting not found"));

        Manager currentManager = getManager();
//        verifyManagerAuthorization(meeting);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        if (!currentManager.equals(meeting.getManager())) {
            throw new BadRequestException("접근 권한이 없습니다.");
        }

        // 동적으로 URL을 생성
        String urlPrefix = String.format("https://kr.object.ncloudstorage.com/meeting%d/", meetingId);

        // S3에서 오디오 파일을 가져옴
        String bucketName = "meeting" + meetingId;
        S3Object s3Object = s3Client.getObject(bucketName, report.getFilePath().replace(urlPrefix,""));
        InputStream inputStream = s3Object.getObjectContent();

        // 오디오 파일을 텍스트로 변환
        TranscriptionResponseDto transcriptionResponse = audioProcessingService.processAudioFromStream(inputStream);

        return ReportDetailResponseDto.fromEntity(report, transcriptionResponse);
    }


    /**
     * 현재 로그인된 사용자의 정보를 가져오는 유틸리티 메서드.
     */
    private User getCurrentUser() {
        String email = SecurityUtil.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 특정 미팅에서 스타 사용자를 찾는 메서드.
     */
    private List<User> findStarInMeeting(Meeting meeting) {

        List<User> stars = userRepository.findByMeeting_MeetingIdAndRole(meeting.getMeetingId(), STAR);

        if (stars.isEmpty()) {
            throw new NotFoundException("해당 미팅에서 스타 사용자를 찾을 수 없습니다.");
        }

        return stars; // 모든 스타 사용자 반환
    }

    private Manager getManager(){
        String email = SecurityUtil.getCurrentUserEmail();
        return managerRepository.findByEmail(email).orElseThrow(()->
                new NotFoundException("manager not found"));
    }

//    /**
//     * 현재 로그인된 관리자가 해당 미팅에 접근 권한이 있는지 확인하는 메서드.
//     */
//    private void verifyManagerAuthorization(Meeting meeting) {
//        String currentManagerEmail = SecurityUtil.getCurrentUserEmail();
//        if (!meeting.getManager().getEmail().equals(currentManagerEmail)) {
//            throw new BadRequestException("접근 권한이 없습니다.");
//        }
//    }
}
