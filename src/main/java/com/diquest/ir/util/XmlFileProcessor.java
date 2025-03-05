package com.diquest.ir.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

import com.diquest.ir5.common.msg.collection.CollectionType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Mariner4 세팅 xml 파일을 읽어와 m5 세팅 파일로 저장 처리하는 클래스
 *
 * @version 1.0
 * @since 2025-02-25
 */
public class XmlFileProcessor {

    private final String ir4HomePath;
    private final String ir5HomePath;

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
                        }
                    }
                    String union = collectionSettingElement.getAttribute("union");
                    String join = collectionSettingElement.getAttribute("join");
                    String type = "GENERAL";
                    if ("true".equalsIgnoreCase(union)) {
                        type = "UNION";
                    } else if ("true".equalsIgnoreCase(join)) {
                        type = "JOIN";
                    }
                    collectionSettingElement.removeAttribute("union");
                    collectionSettingElement.removeAttribute("join");
                    collectionSettingElement.setAttribute("type", type);
                }
            } else if (filePath.contains(DBWATCHER_FOLDER)) {  // setting/COLLECTION_NAME/dbwatcher 폴더 내 xml 파일들
                if (DBWATCHER_LIST.equals(fileName)) {  // setting/COLLECTION_NAME/dbwatcher/list_내_정의된_DBWATCHER_파일.xml
                    // `element` 태그의 `id` 속성을 대문자로 변환
                    NodeList elements = document.getElementsByTagName("element");
                    for (int idx = 0; idx < elements.getLength(); idx++) {
                        Element element = (Element) elements.item(idx);
                        String id = element.getAttribute("id");
                        if (id != null && !id.isEmpty() && id.equals(id.toLowerCase())) {
                            element.setAttribute("id", id.toUpperCase());
                        }
                    }
                    // 변경된 XML 파일 저장
                    String saveFilePath = filePath.replace(inputFilePath, outputFilePath);
                    saveXmlFile(xmlDeclaration, document, saveFilePath);
                    System.out.println("Processed and saved with updated id attributes: " + saveFilePath);
                } else {    // setting/COLLECTION_NAME/dbwatcher/list_내_정의된_DBWATCHER_파일.xml
                    // dbwatcher 설정 파일 처리
                    NodeList idNode = document.getElementsByTagName("id");
                    if (idNode.getLength() > 0) {
                        Element element = (Element) idNode.item(0);
                        String id = element.getTextContent().toUpperCase();
                        Element newIdElement = (Element) element.cloneNode(false);
                        newIdElement.setTextContent(id);
                        replaceElementWithNewTag(element, newIdElement);
                    }

                    NodeList fullCollectSqlPre = document.getElementsByTagName("fullCollectSqlPre");
                    NodeList fullCollectSql = document.getElementsByTagName("fullCollectSql");
                    NodeList fullCollectSqlPost = document.getElementsByTagName("fullCollectSqlPost");
                    NodeList autoUpdateCheckPre = document.getElementsByTagName("autoUpdateCheckPre");
                    NodeList autoUpdateCheckPost = document.getElementsByTagName("autoUpdateCheckPost");
                    NodeList updateIdSelectSql = document.getElementsByTagName("updateIdSelectSql");
                    NodeList incCollectSql = document.getElementsByTagName("incCollectSql");

                    // XML 내용 생성
                    // 전체색인 (sqlFull)
                    boolean hasFullQueryNode = false;
                    Element sqlFull = document.createElement("sqlFull");
                    document.getDocumentElement().appendChild(sqlFull);
                    Element fullQuery = document.createElement("fullQuery");
                    fullQuery.setAttribute("id", "FULL_SQL");
                    if (fullCollectSqlPre.getLength() > 0) {
                        Element element = (Element) fullCollectSqlPre.item(0);
                        String queryText = element.getTextContent();
                        if (!queryText.isEmpty()) {
                            Node preNode = createElementWithText(document, "pre", queryText);
                            fullQuery.appendChild(preNode);
                        }
                        element.getParentNode().removeChild(element);
                        if (!hasFullQueryNode) {
                            hasFullQueryNode = true;
                        }
                    }
                    if (fullCollectSql.getLength() > 0) {
                        Element element = (Element) fullCollectSql.item(0);
                        String queryText = element.getTextContent();
                        if (!queryText.isEmpty()) {
                            Node mainNode = createElementWithText(document, "main", queryText);
                            fullQuery.appendChild(mainNode);
                        }
                        element.getParentNode().removeChild(element);
                        if (!hasFullQueryNode) {
                            hasFullQueryNode = true;
                        }
                    }
                    if (fullCollectSqlPost.getLength() > 0) {
                        Element element = (Element) fullCollectSqlPost.item(0);
                        String queryText = element.getTextContent();
                        if (!queryText.isEmpty()) {
                            Node postNode = createElementWithText(document, "post", queryText);
                            fullQuery.appendChild(postNode);
                        }
                        element.getParentNode().removeChild(element);
                        if (!hasFullQueryNode) {
                            hasFullQueryNode = true;
                        }
                    }
                    if (hasFullQueryNode) {
                        sqlFull.appendChild(fullQuery);
                    }

                    // 추가색인 (incrementalQuery)
                    boolean hasIncrementalQueryNode = false;
                    Element sqlIncremental = document.createElement("sqlIncremental");
                    document.getDocumentElement().appendChild(sqlIncremental);
                    Element incrementalQuery = document.createElement("incrementalQuery");
                    incrementalQuery.setAttribute("id", "INC_SQL");
                    if (autoUpdateCheckPre.getLength() > 0) {
                        Element element = (Element) autoUpdateCheckPre.item(0);
                        String queryText = element.getTextContent();
                        if (!queryText.isEmpty()) {
                            Node preNode = createElementWithText(document, "pre", queryText);
                            incrementalQuery.appendChild(preNode);
                        }
                        element.getParentNode().removeChild(element);
                        if (!hasIncrementalQueryNode) {
                            hasIncrementalQueryNode = true;
                        }
                    }
                    if (updateIdSelectSql.getLength() > 0) {
                        Element element = (Element) updateIdSelectSql.item(0);
                        String queryText = element.getTextContent();
                        if (!queryText.isEmpty()) {
                            Node updateIdNode = createElementWithText(document, "updateId", queryText);
                            incrementalQuery.appendChild(updateIdNode);
                        }
                        element.getParentNode().removeChild(element);
                        if (!hasIncrementalQueryNode) {
                            hasIncrementalQueryNode = true;
                        }
                    }
                    if (incCollectSql.getLength() > 0) {
                        Element element = (Element) incCollectSql.item(0);
                        String queryText = element.getTextContent();
                        if (!queryText.isEmpty()) {
                            Node updateIdNode = createElementWithText(document, "main", queryText);
                            incrementalQuery.appendChild(updateIdNode);
                        }
                        element.getParentNode().removeChild(element);
                        if (!hasIncrementalQueryNode) {
                            hasIncrementalQueryNode = true;
                        }
                    }
                    if (autoUpdateCheckPost.getLength() > 0) {
                        Element element = (Element) autoUpdateCheckPost.item(0);
                        String queryText = element.getTextContent();
                        if (!queryText.isEmpty()) {
                            Node updateIdNode = createElementWithText(document, "post", queryText);
                            incrementalQuery.appendChild(updateIdNode);
                        }
                        element.getParentNode().removeChild(element);
                        if (!hasIncrementalQueryNode) {
                            hasIncrementalQueryNode = true;
                        }
                    }
                    if (hasIncrementalQueryNode) {
                        sqlIncremental.appendChild(incrementalQuery);
                    }

                    // 사용하지 않는 태그 제거
                    NodeList manualUpdateCheckPost = document.getElementsByTagName("manualUpdateCheckPost");
                    if (manualUpdateCheckPost.getLength() > 0) {
                        for (int idx = 0; idx < manualUpdateCheckPost.getLength(); idx++) {
                            Element element = (Element) manualUpdateCheckPost.item(idx);
                            element.getParentNode().removeChild(element);
                        }
                    }
                    NodeList fieldUpdateCollectSqlPre = document.getElementsByTagName("fieldUpdateCollectSqlPre");
                    if (fieldUpdateCollectSqlPre.getLength() > 0) {
                        for (int idx = 0; idx < fieldUpdateCollectSqlPre.getLength(); idx++) {
                            Element element = (Element) fieldUpdateCollectSqlPre.item(idx);
                            element.getParentNode().removeChild(element);
                        }
                    }NodeList fieldUpdateCollectSqlPost = document.getElementsByTagName("fieldUpdateCollectSqlPost");
                    if (fieldUpdateCollectSqlPost.getLength() > 0) {
                        for (int idx = 0; idx < fieldUpdateCollectSqlPost.getLength(); idx++) {
                            Element element = (Element) fieldUpdateCollectSqlPost.item(idx);
                            element.getParentNode().removeChild(element);
                        }
                    }

                    // 새로운 태그 추가
                    Element sqlFieldUpdate = document.createElement("sqlFieldUpdate");
                    document.getDocumentElement().appendChild(sqlFieldUpdate);
                    Element sqlDocAdd = document.createElement("sqlDocAdd");
                    document.getDocumentElement().appendChild(sqlDocAdd);

                    // 변경된 XML 파일 저장 (파일명은 대문자로 수정)
                    String saveFilePath = filePath.replace(inputFilePath, outputFilePath);
                    int dotIndex = fileName.lastIndexOf('.');
                    String nameWithoutExtension = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
                    String fileExtension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);
                    String upperCaseName = nameWithoutExtension.toUpperCase();  // 파일명을 대문자로 변환
                    String repUpperFileName = upperCaseName + fileExtension;
                    saveFilePath = saveFilePath.replace(fileName, repUpperFileName);
                    saveXmlFile(xmlDeclaration, document, saveFilePath);
                    System.out.println("Processed and saved with updated SQL elements: " + saveFilePath);
                }
            }
        } else {
            // 옮길 대상이 아니거나, 아직 이관 개발이 안 된 대상 파일들은 아무것도 하지 않는다.
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
     * 파일을 복사한다.
     *
     * @param sourceFilePath 원본 파일 경로
     * @param destinationFilePath 대상 파일 경로
     * */
    private void copyFile(String sourceFilePath, String destinationFilePath) {
        try {
            Files.createDirectories(Paths.get(destinationFilePath).getParent());
            Files.copy(Paths.get(sourceFilePath), Paths.get(destinationFilePath), StandardCopyOption.REPLACE_EXISTING);
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

    /**
     * 주어진 이름과 텍스트 내용을 가진 새로운 XML 요소를 생성
     *
     * @param doc XML Document 객체
     * @param tagName 태그 이름
     * @param textContent 태그 내용
     * @return 생성된 XML Element 객체
     */
    private Element createElementWithText(Document doc, String tagName, String textContent) {
        Element element = doc.createElement(tagName);
        element.appendChild(doc.createTextNode(textContent));
        return element;
    }

    /**
     * 기존 XML 요소를 새로운 태그 이름을 가진 요소로 대체
     *
     * @param oldElement 기존 XML Element 객체
     * @param newElement 새로운 태그 이름을 가진 XML Element 객체
     */
    private void replaceElementWithNewTag(Element oldElement, Element newElement) {
        Node parentNode = oldElement.getParentNode();
        parentNode.replaceChild(newElement, oldElement);
    }

    private static final String COLLECTION_LIST = "collectionList.xml";
    private static final String DBWATCHER_FOLDER = "dbwatcher";
    private static final String COLLECTION_SETTING = "collectionSetting.xml";
    private static final String PROFILE_SETTING = "profileSetting.xml";
    private static final String DBWATCHER_LIST = "list.xml";
}