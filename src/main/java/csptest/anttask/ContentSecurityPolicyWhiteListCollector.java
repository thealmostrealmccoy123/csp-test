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
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;

/**
 * Lessons learned
 * 1. Java tag libraries embedded in html tags makes it difficult to parse the resource value
 *    E.g. single quote versus double quote used in embedded java tag libraries
 *    
 * 2. Inconsistent html tag usage makes parsing diffult
 *    E.g. single quote versus double quote used to define resource.
 * 
 * @author thealmostrealmccoy
 *
 */
public class ContentSecurityPolicyWhiteListCollector extends MatchingTask {
    
    private static final String SCRIPT_TAG_PATTERN = "<script.*</script>";
    private static final String SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX = "src=\"";
    private static final String SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX = "src='";
    private static final String SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX = "'";    
    private static final String IMAGE_TAG_PATTERN = "<img.*>";
    private static final String IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX = "src=\"";
    private static final String IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX = "src='";
    private static final String IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX = "'";    
    private static final String ANCHOR_TAG_PATTERN = "<a.*</a>";
    private static final String ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX = "href=\"";
    private static final String ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX = "href='";
    private static final String ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX = "'";    
    private static final String FRAME_TAG_PATTERN = "<iframe.*</iframe>";
    private static final String FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX = "src=\"";
    private static final String FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String FRAME_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX = "src='";
    private static final String FRAME_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX = "'";
    private static final String LINK_TAG_PATTERN = "<link.*>";
    private static final String LINK_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX = "href=\"";
    private static final String LINK_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX = "\"";
    private static final String LINK_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX = "href='";
    private static final String LINK_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX = "'";    
    
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
        setResourceParsers(resourceParsers);
    }
    
    public void execute() throws BuildException {
        
        assert getBaseDirectory()!=null;
        
        assert getIncludes()!=null;
        
        DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setIncludes(getIncludes().split(","));
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
    
    private ResourceTypeDetail getResourceTypeDetailInstance() {
        
        return new ResourceTypeDetail() {
            
            private String name;
            
            private String pattern;
            
            private String fileDetail;
            
            private String value;

            @Override
            public void setName(String name) {
                this.name = name;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public void setPattern(String pattern) {
                this.pattern = pattern;
            }

            @Override
            public String getPattern() {
                return pattern;
            }

            @Override
            public void setFileDetail(String fileDetail) {
                this.fileDetail = fileDetail;
            }

            @Override
            public String getFileDetail() {
                return fileDetail;
            }

            @Override
            public void setValue(String value) {
                this.value = value;
            }

            @Override
            public String getValue() {
                return value;
            }
        };
    }
    
    private ResourceParser getScriptTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                assert fileContent!=null;
                
                assert resource!=null;
                
                assert baseDirectory!=null;
                
                assert logger!=null;
                
                List<ResourceTypeDetail> resourceTypeDetails = null;
                
                Pattern pattern = Pattern.compile(SCRIPT_TAG_PATTERN);

                Matcher matcher = pattern.matcher(fileContent);
                
                while (matcher.find()) {
                    
                    String group = matcher.group();
                    
                    if (group!=null) {
                        
                        String scriptTagSrcValue = null;
                        
                        int scriptTagSrcValuePrefixIndex = group.indexOf(SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX);
                        
                        if (scriptTagSrcValuePrefixIndex!=-1) {
                            
                            String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX.length());
                            
                            if (groupSubString!=null) {
                                
                                int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(SCRIPT_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX);
                                
                                if (scriptTagSrcValueSuffixIndex!=-1) {
                                    
                                    scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                    
                                }
                                
                            }
                            
                        } else {
                            
                            scriptTagSrcValuePrefixIndex = group.indexOf(SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX);
                            
                            if (scriptTagSrcValuePrefixIndex!=-1) {
                             
                                String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX.length());
                                
                                if (groupSubString!=null) {
                                    
                                    int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(SCRIPT_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX);
                                    
                                    if (scriptTagSrcValueSuffixIndex!=-1) {
                                        
                                        scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                        
                                    }
                                    
                                }
                                
                            }

                        }
                        
                        if (scriptTagSrcValue!=null) {
                            
                            if (resourceTypeDetails==null) {
                                resourceTypeDetails = new ArrayList<ResourceTypeDetail>();
                            }
                            
                            ResourceTypeDetail resourceTypeDetail = getResourceTypeDetailInstance();
                            resourceTypeDetail.setFileDetail(getFullyQualifiedPath(baseDirectory, resource));
                            resourceTypeDetail.setName("script");
                            resourceTypeDetail.setPattern(SCRIPT_TAG_PATTERN);
                            resourceTypeDetail.setValue(scriptTagSrcValue);
                            
                            resourceTypeDetails.add(resourceTypeDetail);
                            
                            logger.append("File: " + resourceTypeDetail.getFileDetail() + " has " + resourceTypeDetail.getName() + " with value " + resourceTypeDetail.getValue());
                            logger.append(System.getProperty("line.separator"));
                            
                        }
                        
                    }

                }
                
                return resourceTypeDetails;
            }
            
        };
    }
    
    private ResourceParser getAnchorTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                assert fileContent!=null;
                
                assert resource!=null;
                
                assert baseDirectory!=null;
                
                assert logger!=null;
                
                List<ResourceTypeDetail> resourceTypeDetails = null;

                Pattern pattern = Pattern.compile(ANCHOR_TAG_PATTERN);

                Matcher matcher = pattern.matcher(fileContent);
                
                while (matcher.find()) {
                    
                    String group = matcher.group();
                    
                    if (group!=null) {
                        
                        String scriptTagSrcValue = null;
                        
                        int scriptTagSrcValuePrefixIndex = group.indexOf(ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX);
                        
                        if (scriptTagSrcValuePrefixIndex!=-1) {
                            
                            String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX.length());
                            
                            if (groupSubString!=null) {
                                
                                int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(ANCHOR_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX);
                                
                                if (scriptTagSrcValueSuffixIndex!=-1) {
                                    
                                    scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                    
                                }
                                
                            }
                            
                        } else {
                            
                            scriptTagSrcValuePrefixIndex = group.indexOf(ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX);
                            
                            if (scriptTagSrcValuePrefixIndex!=-1) {
                             
                                String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX.length());
                                
                                if (groupSubString!=null) {
                                    
                                    int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(ANCHOR_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX);
                                    
                                    if (scriptTagSrcValueSuffixIndex!=-1) {
                                        
                                        scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                        
                                    }
                                    
                                }
                                
                            }

                        }
                        
                        if (scriptTagSrcValue!=null) {
                            
                            if (resourceTypeDetails==null) {
                                resourceTypeDetails = new ArrayList<ResourceTypeDetail>();
                            }
                            
                            ResourceTypeDetail resourceTypeDetail = getResourceTypeDetailInstance();
                            resourceTypeDetail.setFileDetail(getFullyQualifiedPath(baseDirectory, resource));
                            resourceTypeDetail.setName("anchor");
                            resourceTypeDetail.setPattern(ANCHOR_TAG_PATTERN);
                            resourceTypeDetail.setValue(scriptTagSrcValue);
                            
                            resourceTypeDetails.add(resourceTypeDetail);
                            
                            logger.append("File: " + resourceTypeDetail.getFileDetail() + " has " + resourceTypeDetail.getName() + " with value " + resourceTypeDetail.getValue());
                            logger.append(System.getProperty("line.separator"));
                            
                        }
                        
                    }

                }
                
                return resourceTypeDetails;
            }
            
        };
    }
    
    private ResourceParser getImageTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                assert fileContent!=null;
                
                assert resource!=null;
                
                assert baseDirectory!=null;
                
                assert logger!=null;
                
                List<ResourceTypeDetail> resourceTypeDetails = null;
                
                Pattern pattern = Pattern.compile(IMAGE_TAG_PATTERN);

                Matcher matcher = pattern.matcher(fileContent);
                
                while (matcher.find()) {
                    
                    String group = matcher.group();
                    
                    if (group!=null) {
                        
                        String scriptTagSrcValue = null;
                        
                        int scriptTagSrcValuePrefixIndex = group.indexOf(IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX);
                        
                        if (scriptTagSrcValuePrefixIndex!=-1) {
                            
                            String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX.length());
                            
                            if (groupSubString!=null) {
                                
                                int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(IMAGE_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX);
                                
                                if (scriptTagSrcValueSuffixIndex!=-1) {
                                    
                                    scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                    
                                }
                                
                            }
                            
                        } else {
                            
                            scriptTagSrcValuePrefixIndex = group.indexOf(IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX);
                            
                            if (scriptTagSrcValuePrefixIndex!=-1) {
                             
                                String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX.length());
                                
                                if (groupSubString!=null) {
                                    
                                    int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(IMAGE_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX);
                                    
                                    if (scriptTagSrcValueSuffixIndex!=-1) {
                                        
                                        scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                        
                                    }
                                    
                                }
                                
                            }

                        }
                        
                        if (scriptTagSrcValue!=null) {
                            
                            if (resourceTypeDetails==null) {
                                resourceTypeDetails = new ArrayList<ResourceTypeDetail>();
                            }
                            
                            ResourceTypeDetail resourceTypeDetail = getResourceTypeDetailInstance();
                            resourceTypeDetail.setFileDetail(getFullyQualifiedPath(baseDirectory, resource));
                            resourceTypeDetail.setName("image");
                            resourceTypeDetail.setPattern(IMAGE_TAG_PATTERN);
                            resourceTypeDetail.setValue(scriptTagSrcValue);
                            
                            resourceTypeDetails.add(resourceTypeDetail);
                            
                            logger.append("File: " + resourceTypeDetail.getFileDetail() + " has " + resourceTypeDetail.getName() + " with value " + resourceTypeDetail.getValue());
                            logger.append(System.getProperty("line.separator"));
                            
                        }
                        
                    }

                }
                
                return resourceTypeDetails;
            }
            
        };
    }
    
    private ResourceParser getFrameTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                assert fileContent!=null;
                
                assert resource!=null;
                
                assert baseDirectory!=null;
                
                assert logger!=null;
                
                List<ResourceTypeDetail> resourceTypeDetails = null;
                
                Pattern pattern = Pattern.compile(FRAME_TAG_PATTERN);

                Matcher matcher = pattern.matcher(fileContent);
                
                while (matcher.find()) {
                    
                    String group = matcher.group();
                    
                    if (group!=null) {
                        
                        String scriptTagSrcValue = null;
                        
                        int scriptTagSrcValuePrefixIndex = group.indexOf(FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX);
                        
                        if (scriptTagSrcValuePrefixIndex!=-1) {
                            
                            String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_PREFIX.length());
                            
                            if (groupSubString!=null) {
                                
                                int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(FRAME_TAG_SRC_DOUBLEQUOTE_VALUE_SUFFIX);
                                
                                if (scriptTagSrcValueSuffixIndex!=-1) {
                                    
                                    scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                    
                                }
                                
                            }
                            
                        } else {
                            
                            scriptTagSrcValuePrefixIndex = group.indexOf(FRAME_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX);
                            
                            if (scriptTagSrcValuePrefixIndex!=-1) {
                             
                                String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + FRAME_TAG_SRC_SINGLEQUOTE_VALUE_PREFIX.length());
                                
                                if (groupSubString!=null) {
                                    
                                    int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(FRAME_TAG_SRC_SINGLEQUOTE_VALUE_SUFFIX);
                                    
                                    if (scriptTagSrcValueSuffixIndex!=-1) {
                                        
                                        scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                        
                                    }
                                    
                                }
                                
                            }

                        }
                        
                        if (scriptTagSrcValue!=null) {
                            
                            if (resourceTypeDetails==null) {
                                resourceTypeDetails = new ArrayList<ResourceTypeDetail>();
                            }
                            
                            ResourceTypeDetail resourceTypeDetail = getResourceTypeDetailInstance();
                            resourceTypeDetail.setFileDetail(getFullyQualifiedPath(baseDirectory, resource));
                            resourceTypeDetail.setName("frame");
                            resourceTypeDetail.setPattern(FRAME_TAG_PATTERN);
                            resourceTypeDetail.setValue(scriptTagSrcValue);
                            
                            resourceTypeDetails.add(resourceTypeDetail);
                            
                            logger.append("File: " + resourceTypeDetail.getFileDetail() + " has " + resourceTypeDetail.getName() + " with value " + resourceTypeDetail.getValue());
                            logger.append(System.getProperty("line.separator"));
                            
                        }
                        
                    }

                }
                
                return resourceTypeDetails;
            }
            
        };
    }
    
    private ResourceParser getLinkTagResourceParser() {
        
        return new ResourceParser() {

            @Override
            public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger) {
                
                assert fileContent!=null;
                
                assert resource!=null;
                
                assert baseDirectory!=null;
                
                assert logger!=null;
                
                List<ResourceTypeDetail> resourceTypeDetails = null;

                Pattern pattern = Pattern.compile(LINK_TAG_PATTERN);

                Matcher matcher = pattern.matcher(fileContent);
                
                while (matcher.find()) {
                    
                    String group = matcher.group();
                    
                    if (group!=null) {
                        
                        String scriptTagSrcValue = null;
                        
                        int scriptTagSrcValuePrefixIndex = group.indexOf(LINK_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX);
                        
                        if (scriptTagSrcValuePrefixIndex!=-1) {
                            
                            String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + LINK_TAG_HREF_DOUBLEQUOTE_VALUE_PREFIX.length());
                            
                            if (groupSubString!=null) {
                                
                                int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(LINK_TAG_HREF_DOUBLEQUOTE_VALUE_SUFFIX);
                                
                                if (scriptTagSrcValueSuffixIndex!=-1) {
                                    
                                    scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                    
                                }
                                
                            }
                            
                        } else {
                            
                            scriptTagSrcValuePrefixIndex = group.indexOf(LINK_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX);
                            
                            if (scriptTagSrcValuePrefixIndex!=-1) {
                             
                                String groupSubString = group.substring(scriptTagSrcValuePrefixIndex + LINK_TAG_HREF_SINGLEQUOTE_VALUE_PREFIX.length());
                                
                                if (groupSubString!=null) {
                                    
                                    int scriptTagSrcValueSuffixIndex = groupSubString.indexOf(LINK_TAG_HREF_SINGLEQUOTE_VALUE_SUFFIX);
                                    
                                    if (scriptTagSrcValueSuffixIndex!=-1) {
                                        
                                        scriptTagSrcValue = groupSubString.substring(0, scriptTagSrcValueSuffixIndex);
                                        
                                    }
                                    
                                }
                                
                            }

                        }
                        
                        if (scriptTagSrcValue!=null) {
                            
                            if (resourceTypeDetails==null) {
                                resourceTypeDetails = new ArrayList<ResourceTypeDetail>();
                            }
                            
                            ResourceTypeDetail resourceTypeDetail = getResourceTypeDetailInstance();
                            resourceTypeDetail.setFileDetail(getFullyQualifiedPath(baseDirectory, resource));
                            resourceTypeDetail.setName("link");
                            resourceTypeDetail.setPattern(LINK_TAG_PATTERN);
                            resourceTypeDetail.setValue(scriptTagSrcValue);
                            
                            resourceTypeDetails.add(resourceTypeDetail);
                            
                            logger.append("File: " + resourceTypeDetail.getFileDetail() + " has " + resourceTypeDetail.getName() + " with value " + resourceTypeDetail.getValue());
                            logger.append(System.getProperty("line.separator"));
                            
                        }
                        
                    }

                }
                
                return resourceTypeDetails;
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
    
    private String getFullyQualifiedPath(String baseDirectory, Resource resource) {
        
        if ((baseDirectory!=null)&&(resource!=null)&&(resource.getName()!=null)) {
            
            return baseDirectory + "/" + resource.getName().replaceAll(Matcher.quoteReplacement("\\"), "/");
            
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
        
        String baseDirectory = "C:/eclipse/workspaces/fscm_FS_PASSWD_HASH_UPGRADE";
        String includes = "**\\*.jsp, **\\*.html, **\\*.xml, **\\*.properties";
        ContentSecurityPolicyWhiteListCollector contentSecurityPolicyWhiteListCollector = new ContentSecurityPolicyWhiteListCollector();
        contentSecurityPolicyWhiteListCollector.setBaseDirectory(baseDirectory);
        contentSecurityPolicyWhiteListCollector.setIncludes(includes);
        contentSecurityPolicyWhiteListCollector.execute();
        
/*        String str_1 = "<img src='<c:url value=\"/page_rwd/images/spinning.gif\"></c:url>";
        String str_2 = "<img src=\"<c:url value='/page_rwd/images/icon_alert.gif'></c:url>\" alt=\"ATTENTION\" title=\"ATTENTION\">";
        
        Pattern pattern = Pattern.compile("(src=([\"]))|(src=([']))");
        Matcher matcher = pattern.matcher(str_1);
        
        while (matcher.find()) {
            
            String group = matcher.group();
            
            System.out.println(group);
        }
            
        matcher = pattern.matcher(str_2);
        while (matcher.find()) {
            
            String group = matcher.group();
            
            System.out.println(group);
        }*/
        
    }
    
}