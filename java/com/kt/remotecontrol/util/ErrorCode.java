package com.kt.remotecontrol.util;

import java.util.HashMap;

public class ErrorCode {

    // 1. 정상
    public static final String C000 = "000";// 정상적으로 결과 반환

    // 2. 메시지
    public static final String C101 = "101";// Message 형식 오류
    public static final String C102 = "102";// 명령 코드 오류 - 존재하지 않는 명령
    public static final String C103 = "103";// 필수 항목 누락 ex) HP_NO=
    public static final String C104 = "104";// Node Attribute 오류
    public static final String C105 = "105";// 비밀번호 형식 오류
    public static final String C106 = "106";// 항목의 데이터 길이 오류
    public static final String C107 = "107";// Number형 데이터 오류
    public static final String C108 = "108";// 필수 파라미터(키) 누락 오류 ex)CMD 누락
    public static final String C111 = "111";// 항목의 시간 값 설정 오류 ex) 25
    public static final String C112 = "112";// 정의되지 않은 코드값 입력
    public static final String C199 = "199";// 정의되지 않은 오류 - 메시지 관련 기타 오류

    // 3. 네트워크 오류
    public static final String C201 = "201";// 네트워크 연결 중 오류 발생
    public static final String C202 = "202";// 내부 서버 오류 - HTTP 500 Error: Internal Server Error
    public static final String C203 = "203";// 서비스 불가 - HTTP 503 Error: Service Unavailable
    public static final String C204 = "204";// 다운로드/녹화 프로세스 초기화 실패 - STB 다운로드 에러
    public static final String C205 = "205";// 배포 서버 로그인 실패 - STB 다운로드 에러
    public static final String C206 = "206";// 배포 서버에 해당 파일 존재하지 않음 - STB 다운로드 에러
    public static final String C207 = "207";// 배포 서버 컨텐트 다운로드 시 Read fail - STB 다운로드 에러
    public static final String C299 = "299";// 정의되지 않은 오류 - 네트워크 관련 기타 오류

    // 4. 데이터 베이스
    public static final String C301 = "301";// DB Connection 오류
    public static final String C302 = "302";// 데이터 타입 오류
    public static final String C303 = "303";// 데이터 length 오류
    public static final String C304 = "304";// 검색시 오류
    public static final String C305 = "305";// 중복된 데이터
    public static final String C306 = "306";// 수정, 삭제 오류
    public static final String C307 = "307";// 필수 데이터 누락
    public static final String C308 = "308";// 등록 실패
    public static final String C309 = "309";// 검색 결과 없음
    public static final String C399 = "399";// 정의되지 않은 DB 오류

    // 5. STB
    public static final String C401 = "401";// STB 스위치 OFF
    public static final String C402 = "402";// 내부 네트워크에 존재
    public static final String C403 = "403";// 하드디스크 여유 공간 부족 - STB 다운로드 에러
    public static final String C404 = "404";// STB 서비스 미 적용

    public static final String C405 = "405";// 상품구매 실패 - 기 구매 상품
    public static final String C408 = "408";// 상품 구매 실패 - 기타
    public static final String C409 = "409";// 상품 구매 필요
    public static final String C410 = "410";//  전환 에러
    public static final String C411 = "411";//  대기모드에서 쪽지 수신 불가.
    public static final String C412 = "412";//  채널제한 설정 최대 개수 초과    최대 30개
    public static final String C415 = "415";// 서비스 미가입
    public static final String C417 = "417";// Full Browser상태여서 미러링 URL 실행불가
    public static final String C418 = "418";// 어플 전환 중이여서 미러링 URL 실행불가
    public static final String C419 = "419";// 사진 이미지 에러, 사이즈 너무 큼
    public static final String C420 = "420";// 사진 이미지 에러, 포맷 안 맞음
    public static final String C421 = "421";// 사진 이미지 에러, 지원하지 않는 모델
    public static final String C422 = "422";// 사진 이미지 에러, 기타 오류
    public static final String C499 = "499";// STB 관련 기타 오류

    // 6. 서비스
    public static final String C501 = "501";// HDS 인증 오류
    public static final String C502 = "502";// 비밀 번호 오류
    public static final String C503 = "503";// 중복된 명령
    public static final String C504 = "504";// 유효하지 않은 구매 정보
    public static final String C505 = "505";// 이미 승인/거부 처리된 구매 정보
    public static final String C506 = "506";// HDS Pin(패스워드) 변경 명령 실패
    public static final String C507 = "507";// HDS 핸드폰 번호 목록 요청 명령 실패
    public static final String C508 = "508";// 존재하지 않는 SAID 로 요청
    public static final String C509 = "509";// SAID, 핸드폰 번호 불일치
    public static final String C510 = "510";// 중복 예약
    public static final String C540 = "540";// 자동전원온오프 홈포털연동오류(API 없음)
    public static final String C541 = "541";// 자동전원온오프 홈포털처리오류(API 있음)
    public static final String C542 = "542";// 자동전원온오프 설정값 없음
    public static final String C550 = "550";// 시청시간제한이 걸려 원격명령을 실행할 수 없음
    public static final String C551 = "551";// System.Property의 AL_CF_KTIP_SUPPORT_BTRCU 키값이 support가 아닐경우
    public static final String C552 = "552";// findBTRCU API 호출결과가 false 리턴할 경우
    public static final String C599 = "599";// 정의되지 않은 코드 오류 - 서비스 관련 기타 오류

    // 7. 타 시스템 연동
    public static final String C601 = "601";// HDS 연동 오류
    public static final String C602 = "602";// CMS 연동 오류
    public static final String C603 = "603";// MOC 연동 오류
    public static final String C604 = "604";// SMS 연동 오류
    public static final String C605 = "605";// WAP 연동 오류
    public static final String C606 = "606";// PC 서버 연동 오류
    public static final String C699 = "699";// 타 시스템과 연동 중 오류

    // 8. 제어 서버
    public static final String C999 = "999";// 제어 서버 내부 오류

    public static final String SUCCESS = C000;
    public static final String INVALID_MESSAGE_FORMAT = C101;
    public static final String INVALID_COMMAND = C102;
    public static final String MISSING_REQUIRED_PARAMETER = C103;
    public static final String INVALID_ATTRIBUTE = C104;
    public static final String INVALID_NUMBER_VALUE = C107;
    public static final String STB_ETC = C499;
    public static final String WONT_RUN_CAUSE_LIMITED_WATCHING_TIME = C550;
    public static final String UNDEFINED = C599;

    private static HashMap errorHash = new HashMap();

    static {
        // 1. 정상
        errorHash.put(C000, "정상적으로 결과 반환");

        // 2. 메시지
        errorHash.put(C101, "Message 형식 오류");
        errorHash.put(C102, "명령 코드 오류 - 존재하지 않는 명령");
        errorHash.put(C103, "필수 항목 누락 ex) HP_NO=");
        errorHash.put(C104, "Node Attribute 오류");
        errorHash.put(C105, "비밀번호 형식 오류");
        errorHash.put(C106, "항목의 데이터 길이 오류");
        errorHash.put(C107, "Number형 데이터 오류");
        errorHash.put(C108, "필수 파라미터(키) 누락 오류 ex)CMD 누락");
        errorHash.put(C111, "항목의 시간 값 설정 오류 ex) 25");
        errorHash.put(C112, "정의되지 않은 코드값 입력");
        errorHash.put(C199, "정의되지 않은 오류 - 메시지 관련 기타 오류");

        // 3. 네트워크 오류
        errorHash.put(C201, "네트워크 연결 중 오류 발생");
        errorHash.put(C202, "내부 서버 오류 - HTTP 500 Error: Internal Server Error");
        errorHash.put(C203, "서비스 불가 - HTTP 503 Error: Service Unavailable");
        errorHash.put(C204, "다운로드/녹화 프로세스 초기화 실패 - STB 다운로드 에러");
        errorHash.put(C205, "배포 서버 로그인 실패 - STB 다운로드 에러");
        errorHash.put(C206, "배포 서버에 해당 파일 존재하지 않음 - STB 다운로드 에러");
        errorHash.put(C207, "배포 서버 컨텐트 다운로드 시 Read fail - STB 다운로드 에러");
        errorHash.put(C299, "정의되지 않은 오류 - 네트워크 관련 기타 오류");

        // 4. 데이터 베이스
        errorHash.put(C301, "DB Connection 오류");
        errorHash.put(C302, "데이터 타입 오류");
        errorHash.put(C303, "데이터 length 오류");
        errorHash.put(C304, "검색시 오류");
        errorHash.put(C305, "중복된 데이터");
        errorHash.put(C306, "수정, 삭제 오류");
        errorHash.put(C307, "필수 데이터 누락");
        errorHash.put(C308, "등록 실패");
        errorHash.put(C309, "검색 결과 없음");
        errorHash.put(C399, "정의되지 않은 DB 오류");

        // 5. STB
        errorHash.put(C401, "STB 스위치 OFF");
        errorHash.put(C402, "내부 네트워크에 존재");
        errorHash.put(C403, "하드디스크 여유 공간 부족 - STB 다운로드 에러");
        errorHash.put(C404, "STB 서비스 미 적용");
        errorHash.put(C410, "전환 시 오류로 인한 전환 실패");
        errorHash.put(C415, "서비스 미가입");
        errorHash.put(C417, "Full Browser상태여서 미러링 URL 실행불가");
        errorHash.put(C418, "어플 전환 중이여서 미러링 URL 실행불가");

        errorHash.put(C499, "STB 관련 기타 오류");

        // 6. 서비스
        errorHash.put(C501, "HDS 인증 오류");
        errorHash.put(C502, "비밀 번호 오류");
        errorHash.put(C503, "중복된 명령");
        errorHash.put(C504, "유효하지 않은 구매 정보");
        errorHash.put(C505, "이미 승인/거부 처리된 구매 정보");
        errorHash.put(C506, "HDS Pin(패스워드) 변경 명령 실패");
        errorHash.put(C507, "HDS 핸드폰 번호 목록 요청 명령 실패");
        errorHash.put(C508, "존재하지 않는 SAID 로 요청");
        errorHash.put(C509, "SAID, 핸드폰 번호 불일치");
        errorHash.put(C510, "중복 예약 오류");
        errorHash.put(C550, "시청시간제한이 걸려 원격명령을 실행할 수 없음");
        errorHash.put(C551, "AL_CF_KTIP_SUPPORT_BTRCU 키값이 support가 아닐 경우");
        errorHash.put(C552, "findBTRCU API 호출결과가 false 리턴할 경우");
        errorHash.put(C599, "정의되지 않은 코드 오류 - 서비스 관련 기타 오류");

        // 7. 타 시스템 연동
        errorHash.put(C601, "HDS 연동 오류");
        errorHash.put(C602, "CMS 연동 오류");
        errorHash.put(C603, "MOC 연동 오류");
        errorHash.put(C604, "SMS 연동 오류");
        errorHash.put(C605, "WAP 연동 오류");
        errorHash.put(C606, "PC 서버 연동 오류");
        errorHash.put(C699, "타 시스템과 연동 중 오류");

        // 8. 제어 서버
        errorHash.put(C999, "제어 서버 내부 오류");
    }

    public static String getErrorMessage(String code) {
        return (String) errorHash.get(code);
    }

}
