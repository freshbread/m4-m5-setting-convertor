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
     */
    public static void main(String[] args) {
        String ir4HomePath = "/home/bmt2023/saup3/mariner4";
        String ir5HomePath = "/home/bmt_saup3/mariner5";
        XmlFileProcessor xmlFileProcessor = new XmlFileProcessor(ir4HomePath, ir5HomePath);
        // TODO 1. 기존 m4 세팅을 가져온다.
//        String beforeSettingsFolderPath = args[0];
        String beforeSettingsFolderPath = "C:\\Users\\white\\TEST_HOME\\tmp\\m4\\setting";
        // TODO 2. m4 세팅을 가져와서 m5 세팅에 맞게 수정한다.
//        String beforeSettingsFolderPath = args[1];
        String convertSettingsFolderPath = "C:\\Users\\white\\TEST_HOME\\tmp\\m5\\setting";
        // TODO 3. 수정된 내용을 m5 세팅 형식에 맞게 저장한다.
        xmlFileProcessor.convertSettingFiles(beforeSettingsFolderPath, convertSettingsFolderPath);
    }
}
