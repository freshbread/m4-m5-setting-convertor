package com.diquest.ir.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Mariner4 세팅 xml 파일을 읽어와 m5 세팅 파일로 저장 처리하는 클래스
 *
 * @author jhjeon
 * @version 1.0
 * @since 2025-02-25
 */
public class XmlFileProcessor {

    private String ir4HomePath;
    private String ir5HomePath;

    public XmlFileProcessor() {
        ir4HomePath = System.getProperty("IR4_HOME");
        ir5HomePath = System.getProperty("IR5_HOME");
    }

    public XmlFileProcessor(String ir4HomePath, String ir5HomePath) {
        this.ir4HomePath = ir4HomePath;
        this.ir5HomePath = ir5HomePath;
    }

    /**
     * 기존 m4 세팅을 가져와 m5 세팅으로 변경하는 메인 함수
     *
     * @param inputFilePath     기존 XML 파일이 있는 폴더 경로
     * @param outputFilePath    출력 파일 경로
     * */
    public void convertSettingFiles(String inputFilePath, String outputFilePath) {
        try {
            List<File> xmlFiles = getXmlFiles(inputFilePath);
            for (File xmlFile : xmlFiles) {
                saveConvertedSettingFile(xmlFile, inputFilePath, outputFilePath);
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
    }

    /**
     * XML 파일 목록을 가져온다.
     *
     * @param inputFilePath 기존 XML 파일이 있는 폴더 경로
     * @return 변경 대상 XML 파일 목록
     * */
    private List<File> getXmlFiles(String inputFilePath) throws IOException {
        return Files.walk(Paths.get(inputFilePath))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".xml"))
                .filter(path -> !path.getFileName().toString().startsWith("old_"))
                .map(java.nio.file.Path::toFile)
                .collect(Collectors.toList());
    }

    /**
     * 변경할 XML 파일을 체크 및 변경작업 후 저장한다.
     *
     * @param xmlFile   변경 대상 설정 XML 파일
     * @param inputFilePath 기존 XML 파일이 있던 setting 폴더 경로
     * @param outputFilePath    새로 생성될 XML 파일이 저장될 setting 폴더 경로
     * */
    private void saveConvertedSettingFile(
            File xmlFile,
            String inputFilePath,
            String outputFilePath
    ) throws ParserConfigurationException, IOException, SAXException {
        String fileName = xmlFile.getName();
        String filePath = xmlFile.getPath();
        if (
            COLLECTION_LIST.equals(fileName)    // setting/collectionList.xml
            || PROFILE_SETTING.equals(fileName)    // setting/COLLECTION_NAME/profileSetting.xml
            || filePath.contains(DBWATCHER_FOLDER) && DBWATCHER_LIST.equals(fileName)   // setting/COLLECTION_NAME/dbwatcher/list.xml
        ) { // 딱히 m4, m5 간의 차이가 보이지 않으므로 그대로 복사한다.
            String destinationFilePath = filePath.replace(inputFilePath, outputFilePath);
            copyFile(filePath, destinationFilePath);
            System.out.println("Copied: " + destinationFilePath);
        } else if (
            COLLECTION_SETTING.equals(fileName) // setting/COLLECTION_NAME/collectionSetting.xml
            || filePath.contains(DBWATCHER_FOLDER)  // setting/COLLECTION_NAME/dbwatcher 폴더 내 xml 파일들
        ) {
            // XML 파일 내용을 읽어온다.
            // XML 선언 태그 읽기
            String xmlDeclaration = readXmlDeclaration(filePath);// XML 파일 읽기
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document document = dBuilder.parse(xmlFile);
            document.getDocumentElement().normalize();
            if (COLLECTION_SETTING.equals(fileName)) {  // setting/COLLECTION_NAME/collectionSetting.xml
                // 설정내용 중 collectionSetting 내의 baseDirectory 속성 값의 경로를 수정한다. ($IR4_HOME > $IR5_HOME)
                NodeList collectionSettingNodeList = document.getElementsByTagName("collectionSetting");
                if (collectionSettingNodeList.getLength() > 0) {
                    Element collectionSettingElement = (Element) collectionSettingNodeList.item(0);
                    String baseDirectory = collectionSettingElement.getAttribute("baseDirectory");
                    if (ir4HomePath != null && !ir4HomePath.isEmpty()) {
                        if (baseDirectory.contains(ir4HomePath)) {
                            baseDirectory = baseDirectory.replace(ir4HomePath, ir5HomePath);
                            collectionSettingElement.setAttribute("baseDirectory", baseDirectory);
                            // 변경된 collectionSetting.xml 내용을 m5 세팅을 저장할 별도 폴더에 저장한다.
                            String saveFilePath = filePath.replace(inputFilePath, outputFilePath);
                            saveXmlFile(xmlDeclaration, document, saveFilePath);
                            System.out.println("Processed and saved: " + saveFilePath);
                        } else {
                            // baseDirectory에 ir4HomePath 경로가 포함되지 않은 경우 baseDirectory 내용을 바꿀 수 없으므로 파일 복사
                            String destinationFilePath = filePath.replace(inputFilePath, outputFilePath);
                            copyFile(filePath, destinationFilePath);
                            System.out.println("Copied: " + destinationFilePath);
                        }
                    } else {
                        // ir4HomePath가 null이거나 비어있는 경우 baseDirectory 내용을 바꿀 수 없으므로 파일 복사
                        String destinationFilePath = filePath.replace(inputFilePath, outputFilePath);
                        copyFile(filePath, destinationFilePath);
                        System.out.println("Copied: " + destinationFilePath);
                    }
                }
            } else if (filePath.contains(DBWATCHER_FOLDER) && !DBWATCHER_LIST.equals(fileName)) {   // setting/COLLECTION_NAME/dbwatcher/list_내_정의된_DBWATCHER_파일.xml
                // TODO dbwatcher 설정 파일 처리
            } else if (filePath.contains(DBWATCHER_FOLDER) && !DBWATCHER_LIST.equals(fileName)) {   // setting/COLLECTION_NAME/dbwatcher/list_내_정의된_DBWATCHER_파일.xml
                // TODO dbwatcher 설정 파일 처리
            }
        } else {
            // 옮길 대상이 아니거나, 아직 이관 개발이 안 된 대상 파일들은 아무것도 하지 않는다.
        }
    }

    /**
     * 파일을 복사한다.
     *
     * @param sourceFilePath 원본 파일 경로
     * @param destinationFilePath 대상 파일 경로
     * */
    private void copyFile(String sourceFilePath, String destinationFilePath) {
        try {
            Files.createDirectories(Paths.get(destinationFilePath).getParent());
            Files.copy(Paths.get(sourceFilePath), Paths.get(destinationFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * XML 기본 선언 태그 가져오기
     *
     * @param filePath XML 파일 경로
     * @return 읽어온 XML 선언 태그 (없을 경우 기본 XML 선언 태그)
     * */
    private String readXmlDeclaration(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String firstLine = br.readLine();
            if (firstLine != null && firstLine.startsWith("<?xml")) {
                return firstLine;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; // 기본 XML 선언 태그
    }

    /**
     * XML 파일을 저장한다.
     *
     * @param xmlDeclaration XML 선언 태그
     * @param doc XML Document 객체
     * @param filePath XML 파일을 저장할 경로
     * */
    private void saveXmlFile(String xmlDeclaration, Document doc, String filePath) {
        try {
            // 경로가 존재하지 않을 경우 생성
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 임시 파일에 저장
            File tempFile = new File(filePath + ".tmp");
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(tempFile);
            transformer.transform(source, result);

            // 최종 파일에 XML 선언 태그와 함께 저장
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println(xmlDeclaration);
                try (BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        writer.println(line);
                    }
                }
            }

            // 임시 파일 삭제
            tempFile.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final String COLLECTION_LIST = "collectionList.xml";
    private static final String DBWATCHER_FOLDER = "dbwatcher";
    private static final String COLLECTION_SETTING = "collectionSetting.xml";
    private static final String PROFILE_SETTING = "profileSetting.xml";
    private static final String DBWATCHER_LIST = "list.xml";
}