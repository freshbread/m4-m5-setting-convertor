package com.diquest.ir.util;

/**
 * Mariner Setting Convertor 실행 클래스 (m4 > m5)
 *
 * @author jhjeon
 * @version 1.0
 * @since 2025-02-25
 */
public class MarinerSettingConvertor {

    /**
     * Convertor 실행 메인 함수
     *
     * @param args 실행 시 전달되는 인자들입니다.
     *             args[0] ir4HomePath: 기존 mariner4 경로 (IR4_HOME 시스템 설정값)
     *             args[1] ir5HomePath: mariner5 경로
     *             args[2] beforeSettingsFolderPath: 기존 mariner4 setting 폴더
     *             args[3] convertSettingsFolderPath: 변환된 mariner5 setting 파일을 저장할 폴더
     */
    public static void main(String[] args) {
        XmlFileProcessor xmlFileProcessor = null;
        if (args.length >= 4) {
            String ir4HomePath = args[0];
            String ir5HomePath = args[1];
            String beforeSettingsFolderPath = args[2];
            String convertSettingsFolderPath = args[3];
            xmlFileProcessor = new XmlFileProcessor(ir4HomePath, ir5HomePath);
            xmlFileProcessor.convertSettingFiles(beforeSettingsFolderPath, convertSettingsFolderPath);
        } else {
            System.out.println("변환을 하기에 필요한 파라미터 입력이 부족합니다.");
        }
    }
}
