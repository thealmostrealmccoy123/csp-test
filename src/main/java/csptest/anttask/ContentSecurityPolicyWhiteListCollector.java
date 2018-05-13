/**
 * Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package csptest.anttask;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import csptest.common.ContentSecurityPolicyFactory;
import csptest.common.ResourceParser;
import csptest.common.ResourceType;
import csptest.common.ResourceTypeDetail;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

/**
 * Lessons learned
 * 1. Java tag libraries embedded in html tags makes it difficult to parse the resource value
 *    E.g. single quote versus double quote used in embedded java tag libraries
 *    
 * 2. Inconsistent html tag usage makes parsing difficult
 *    E.g. single quote versus double quote used to define resource.
 * 
 * @author thealmostrealmccoy
 *
 */
public class ContentSecurityPolicyWhiteListCollector extends MatchingTask {
    
    private static final String SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX = "src=\"";
    private static final String SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX = "src='";
    private static final String SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX = "'";    

    private static final String IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX = "src=\"";
    private static final String IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX = "src='";
    private static final String IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX = "'";    

    private static final String ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX = "href=\"";
    private static final String ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX = "href='";
    private static final String ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX = "'";    
    
    private static final String FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX = "src=\"";
    private static final String FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String FRAME_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX = "src='";
    private static final String FRAME_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX = "'";

    private static final String LINK_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX = "href=\"";
    private static final String LINK_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String LINK_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX = "href='";
    private static final String LINK_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX = "'";
    private static final String LINK_TAG_MANIFEST_DOUBLEQUOTE_VALUE = "rel=\"manifest\"";
    private static final String LINK_TAG_MANIFEST_SINGLEQUOTE_VALUE = "rel='manifest'";
    
    private static final String STYLE_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX = LINK_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX;
    private static final String STYLE_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX = LINK_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX;
    private static final String STYLE_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX = LINK_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX;
    private static final String STYLE_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX = LINK_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX;
    private static final String STYLE_TAG_STYLESHEET_DOUBLEQUOTE_VALUE = "rel=\"stylesheet\"";
    private static final String STYLE_TAG_STYLESHEET_SINGLEQUOTE_VALUE = "rel='stylesheet'";    
    
    private static final String HTTP_MATCHER_REGEX = "^(http|https)://";
    
    private String baseDirectory;
    
    private String includes;
    
    private List<ResourceParser> resourceParsers;
    
    public ContentSecurityPolicyWhiteListCollector() {
        
        List<ResourceParser> resourceParsers = new ArrayList<ResourceParser>();
        resourceParsers.add(getScriptTagResourceParser());
        resourceParsers.add(getAnchorTagResourceParser());
        resourceParsers.add(getImageTagResourceParser());
        resourceParsers.add(getFrameTagResourceParser());
        resourceParsers.add(getLinkTagResourceParser());
        resourceParsers.add(getStyleTagResourceParser());
        setResourceParsers(resourceParsers);
    }
    
    public void execute() throws BuildException {
        
        assert getBaseDirectory()!=null;
        
        assert getIncludes()!=null;
        
        DirectoryScanner directoryScanner = new DirectoryScanner();
        
        if (getIncludes()!=null) {
            directoryScanner.setIncludes(splitAndTrim());    
        }
        
        directoryScanner.setBasedir(new File(getBaseDirectory()));
        directoryScanner.scan();
        
        StringBuilder str = new StringBuilder();
        String[] files = directoryScanner.getIncludedFiles();
        
        if ((files!=null)&&(files.length>0)&&(getResourceParsers()!=null)&&(!getResourceParsers().isEmpty())) {
            
            List<ResourceTypeDetail> resourceTypeDetails = null;
            
            str.append(System.getProperty("line.separator"));
            str.append(System.getProperty("line.separator"));
            str.append("******************* FILES: *******");
            str.append(System.getProperty("line.separator"));
            
            for (int i = 0; i < files.length; i++) {
                
                Resource resource = directoryScanner.getResource(files[i]);
                
                assert resource!=null;
                
                if (resource!=null && !resource.isDirectory()) {
                    
                    try {
                        
                        InputStream resourceInputStream = resource.getInputStream();
                        
                        String fileContent = getInputStreamContentAsString(resourceInputStream);
                        
                        for (ResourceParser resourceParser: resourceParsers) {

                            assert resourceParser!=null;
                            
                            List<ResourceTypeDetail> parsedResourceTypeDetails = resourceParser.parseResource(fileContent, resource, getBaseDirectory(), str);
                            
                            if ((parsedResourceTypeDetails!=null)&&(!parsedResourceTypeDetails.isEmpty())) {
                                
                                if (resourceTypeDetails==null) {
                                    
                                    resourceTypeDetails = new ArrayList<ResourceTypeDetail>();
                                }
                                
                                resourceTypeDetails.addAll(parsedResourceTypeDetails);
                            }

                        }
                                               
                    } catch (IOException e) {
                        
                        log(e, Project.MSG_ERR);
                    }
                }
                
            }
            
            generateXMLReport(resourceTypeDetails);
            
            log(str.toString(), Project.MSG_DEBUG);
        }
        
    }
    
    private void generateXMLReport(List<ResourceTypeDetail> resourceTypeDetails) {
        
        if ((resourceTypeDetails!=null)&&(!resourceTypeDetails.isEmpty())) {
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db;
            
            try {
                
                db = dbf.newDocumentBuilder();
                Document doc = db.newDocument();
                Element root = doc.createElementNS(null, "resourceType");
                
                for (ResourceTypeDetail resourceTypeDetail: resourceTypeDetails) {
                    
                    Element item = doc.createElementNS(null, "resourceTypeDetail");
                    
                    Element name = doc.createElementNS(null, "name");
                    name.appendChild( doc.createTextNode(resourceTypeDetail.getName()) );
                    item.appendChild(name);
                    
                    Element pattern = doc.createElementNS(null, "pattern");
                    pattern.appendChild( doc.createCDATASection(resourceTypeDetail.getPattern()) );
                    item.appendChild(pattern);

                    Element fileDetail = doc.createElementNS(null, "fileDetail");
                    fileDetail.appendChild( doc.createCDATASection(resourceTypeDetail.getFileDetail()) );
                    item.appendChild(fileDetail);

                    Element value = doc.createElementNS(null, "value");
                    value.appendChild( doc.createCDATASection(resourceTypeDetail.getValue()) );
                    item.appendChild(value);
                    
                    root.appendChild( item );
                }
                
                doc.appendChild( root );
                
                
                OutputStream os = Files.newOutputStream(Paths.get(getBaseDirectory() + "/csp_external_resource_list.xml"), StandardOpenOption.CREATE);
                Result result = new StreamResult(new OutputStreamWriter(os, "utf-8"));
                DOMSource source = new DOMSource(doc);
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer xformer = factory.newTransformer();
                xformer.setOutputProperty(OutputKeys.INDENT, "yes");
                xformer.transform(source, result);
                
                
            } catch (ParserConfigurationException|ClassCastException|IOException|TransformerException e) {
                log(e, Project.MSG_ERR);
            }
            
        }
        
    }
    
    private ResourceParser getScriptTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
               return parseResourceToResourceTypeDetail(fileContent, resource, baseDirectory, logger, ResourceType.SCRIPT, SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX, SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX, SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX, SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX);
            }
            
        };
    }
    
    private ResourceParser getAnchorTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                return parseResourceToResourceTypeDetail(fileContent, resource, baseDirectory, logger, ResourceType.ANCHOR, ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX, ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX, ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX, ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX);
            }
            
        };
    }
    
    private ResourceParser getImageTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                return parseResourceToResourceTypeDetail(fileContent, resource, baseDirectory, logger, ResourceType.IMAGE, IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX, IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX, IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX, IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX);
            }
            
        };
    }
    
    private ResourceParser getFrameTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
            
                return parseResourceToResourceTypeDetail(fileContent, resource, baseDirectory, logger, ResourceType.FRAME, FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX, FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX, FRAME_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX, FRAME_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX);
            }
            
        };
    }
    
    private boolean doesStringContainsHttpProtocol(String srcValue) {
        
        if ((srcValue!=null)&&(!srcValue.trim().equals(""))) {
            
            Pattern pattern = Pattern.compile(HTTP_MATCHER_REGEX);
            
            Matcher matcher = pattern.matcher(srcValue);
            
            if (matcher.find()) {
                return true;
            }
        }
        
        return false;
    }
    
    private List<ResourceTypeDetail> parseResourceToResourceTypeDetail(String fileContent, Resource resource, String baseDirectory, StringBuilder logger, ResourceType resourceType, String doubleQuoteValuePrefix, String doubleQuoteValueSuffix, String singleQuoteValuePrefix, String singleQuoteValueSuffix, String typeAttributeDoubleQuoteValue, String typeAttributeSingleQuoteValue) {
        
        assert fileContent!=null;
        
        assert resource!=null;
        
        assert baseDirectory!=null;
        
        assert logger!=null;
        
        assert resourceType!=null;
        
        assert doubleQuoteValuePrefix!=null;

        assert doubleQuoteValueSuffix!=null;

        assert singleQuoteValuePrefix!=null;

        assert singleQuoteValueSuffix!=null;
        
        List<ResourceTypeDetail> resourceTypeDetails = null;
        
        Pattern pattern = Pattern.compile(resourceType.getPattern(), Pattern.DOTALL);

        Matcher matcher = pattern.matcher(fileContent);
        
        while (matcher.find()) {
            
            String group = matcher.group();
            
            if ((group != null)&&(((typeAttributeDoubleQuoteValue == null) || ((typeAttributeDoubleQuoteValue != null) && (group.contains(typeAttributeDoubleQuoteValue)))) || ((typeAttributeSingleQuoteValue == null) || ((typeAttributeSingleQuoteValue != null) && (group.contains(typeAttributeSingleQuoteValue)))))) {
                
                String tagSrcValue = null;
                
                String tagPrefixPattern = null;
                
                String tagSuffixPattern = null;
                
                int tagSrcValuePrefixIndex = group.indexOf(doubleQuoteValuePrefix);
                
                if (tagSrcValuePrefixIndex!=-1) {
                    
                    tagPrefixPattern = doubleQuoteValuePrefix;
                    
                    tagSuffixPattern = doubleQuoteValueSuffix;
                    
                } else {
                    
                    tagSrcValuePrefixIndex = group.indexOf(singleQuoteValuePrefix);
                    
                    if (tagSrcValuePrefixIndex!=-1) {
                        
                        tagPrefixPattern = singleQuoteValuePrefix;
                        
                        tagSuffixPattern = singleQuoteValueSuffix;
                    }
                }
                
                if (tagSrcValuePrefixIndex!=-1) {
                    
                    String groupSubString = group.substring(tagSrcValuePrefixIndex + tagPrefixPattern.length());
                    
                    if (groupSubString!=null) {
                        
                        int tagSrcValueSuffixIndex = groupSubString.indexOf(tagSuffixPattern);
                        
                        if (tagSrcValueSuffixIndex!=-1) {
                            
                            tagSrcValue = groupSubString.substring(0, tagSrcValueSuffixIndex);
                            
                        }
                        
                    }
                    
                }
                
                if ((tagSrcValue!=null)&&(doesStringContainsHttpProtocol(tagSrcValue))) {
                    
                    if (resourceTypeDetails==null) {
                        resourceTypeDetails = new ArrayList<ResourceTypeDetail>();
                    }
                    
                    ResourceTypeDetail resourceTypeDetail = ContentSecurityPolicyFactory.INSTANCE.getResourceTypeDetailInstance();
                    resourceTypeDetail.setFileDetail(getFullyQualifiedPath(baseDirectory, resource));
                    resourceTypeDetail.setName(resourceType.getName());
                    resourceTypeDetail.setPattern(resourceType.getPattern());
                    resourceTypeDetail.setValue(tagSrcValue);
                    
                    resourceTypeDetails.add(resourceTypeDetail);
                    
                    logger.append("File: " + resourceTypeDetail.getFileDetail() + " has " + resourceTypeDetail.getName() + " with value " + resourceTypeDetail.getValue());
                    logger.append(System.getProperty("line.separator"));
                    
                }
                
            }

        }
        
        return resourceTypeDetails;
    }
    
    
    private List<ResourceTypeDetail> parseResourceToResourceTypeDetail(String fileContent, Resource resource, String baseDirectory, StringBuilder logger, ResourceType resourceType, String doubleQuoteValuePrefix, String doubleQuoteValueSuffix, String singleQuoteValuePrefix, String singleQuoteValueSuffix) {
        
        return parseResourceToResourceTypeDetail(fileContent, resource, baseDirectory, logger, resourceType, doubleQuoteValuePrefix, doubleQuoteValueSuffix, singleQuoteValuePrefix, singleQuoteValueSuffix, null, null);
    }
    
    private ResourceParser getLinkTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                return parseResourceToResourceTypeDetail(fileContent, resource, baseDirectory, logger, ResourceType.LINK, LINK_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX, LINK_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX, LINK_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX, LINK_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX, LINK_TAG_MANIFEST_DOUBLEQUOTE_VALUE, LINK_TAG_MANIFEST_SINGLEQUOTE_VALUE);
            }
            
        };
    }    
    
    private ResourceParser getStyleTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                return parseResourceToResourceTypeDetail(fileContent, resource, baseDirectory, logger, ResourceType.STYLE, STYLE_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX, STYLE_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX, STYLE_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX, STYLE_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX, STYLE_TAG_STYLESHEET_DOUBLEQUOTE_VALUE, STYLE_TAG_STYLESHEET_SINGLEQUOTE_VALUE);
            }
            
        };
    }    
    
    private String getInputStreamContentAsString(InputStream inputStream) {
        
        assert inputStream!=null;
        
        StringBuilder rawContent = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {

            int c = bufferedReader.read();
            while (c != -1) {

                if (rawContent == null) {
                    rawContent = new StringBuilder();
                }
                rawContent.append((char) c);

                c = bufferedReader.read();
            }
            
        } catch (IOException e) {
            
            log(e, Project.MSG_ERR);

        }
        
        if (rawContent!=null) {

            return rawContent.toString();
            
        }
        
        return null;
    }
    
/*    private void scriptTest() {
        
        File f = new File("C:/eclipse/workspaces/fscm_FS_PASSWD_HASH_UPGRADE/src/main/webapp/WEB-INF/jsp/account/careers.jsp");
        
        StringBuilder rawContent = null;

        try (BufferedReader bufferedReader = new BufferedReader(new java.io.FileReader(f))) {

            int c = bufferedReader.read();
            while (c != -1) {

                if (rawContent == null) {
                    rawContent = new StringBuilder();
                }
                rawContent.append((char) c);

                c = bufferedReader.read();
            }
            
        } catch (IOException e) {
            
            log(e, Project.MSG_ERR);

        }
        
        if ((rawContent!=null)&&(rawContent.length()>0)) {
            
            //Pattern pattern = Pattern.compile("(?s)<script\\s(.*?)</script>");
            //Pattern.DOTALL
            Pattern pattern = Pattern.compile("<script.*?</script>", Pattern.DOTALL);
            
            Matcher matcher = pattern.matcher(rawContent);
            
            while (matcher.find()) {
                
                String group = matcher.group();                
                
                System.out.println(group);
            }
            
        }
        
    }*/
    
    private String getFullyQualifiedPath(String baseDirectory, Resource resource) {
        
        if ((baseDirectory!=null)&&(resource!=null)&&(resource.getName()!=null)) {
            
            return baseDirectory + "/" + resource.getName().replaceAll(Matcher.quoteReplacement("\\"), "/");
            
        }
        
        return null;
    }
    
    private String[] splitAndTrim() {
        
        if ((getIncludes()!=null)) {
            
            String[] splitStrings = getIncludes().split(",");
            
            if ((splitStrings!=null)&&(splitStrings.length>0)) {
                
                for (int i=0; i<splitStrings.length; i++) {
                    
                    if (splitStrings[i]!=null) {
                        
                        splitStrings[i] = splitStrings[i].trim();
                    }
                }
                
                return splitStrings;
            }
            
        }
        
        return null;
    }
    
    private String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    private String getIncludes() {
        return includes;
    }

    public void setIncludes(String includes) {
        this.includes = includes;
    }
    
    private List<ResourceParser> getResourceParsers() {
        return resourceParsers;
    }

    private void setResourceParsers(List<ResourceParser> resourceParsers) {
        this.resourceParsers = resourceParsers;
    }

    public static void main(String[] argc ) {
        
        String baseDirectory = "C:/eclipse/workspaces/csp-test";
        String includes = "**\\*.jsp, **\\*.html, **\\*.xml, **\\*.properties, **\\*.js, **\\*.css";
        ContentSecurityPolicyWhiteListCollector contentSecurityPolicyWhiteListCollector = new ContentSecurityPolicyWhiteListCollector();
        contentSecurityPolicyWhiteListCollector.setBaseDirectory(baseDirectory);
        contentSecurityPolicyWhiteListCollector.setIncludes(includes);
        contentSecurityPolicyWhiteListCollector.execute();
    }
    
}