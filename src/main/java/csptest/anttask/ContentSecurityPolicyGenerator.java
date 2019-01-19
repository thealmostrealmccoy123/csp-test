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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.apache.commons.digester.Digester;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import csptest.common.ContentSecurityPolicy;
import csptest.common.PolicyDetail;
import csptest.common.PolicyHeader;
import csptest.common.ResourceType;
import csptest.common.ResourceTypeDetail;
import csptest.common.ResourceTypeDetailDigesterFactory;

/**
 * @author thealmostrealmccoy
 *
 */
public class ContentSecurityPolicyGenerator extends Task {
    
    private static final String CONTENT_SECURITY_POLICY_KEY = "Content-Security-Policy"; 
    
    private static final String CONTENT_SECURITY_POLICY_REPORT_ONLY_KEY = "Content-Security-Policy-Report-Only"; 
    
    private String resourceListLocation;
    
    private String nonce;
    
    private String reportOnly;
    
    private String reportUri;
    
    @Override
    public void execute() throws BuildException {
        
        List resourceTypeList = getResourceTypeDetailListAsObject();
        
        Map<ResourceType, List<ResourceTypeDetail>> resourceTypeListMap = buildMapFromFilteredListByResourceType(resourceTypeList);
        
        Map<ResourceType, Set<String>> resourceTypePolicyMap = buildResourceTypePolicy(resourceTypeListMap);
        
        ContentSecurityPolicy contentSecurityPolicy = buildContentSecurityPolicy(resourceTypePolicyMap);
        
        generateContentSecurityPolicy(contentSecurityPolicy, new File(getResourceListLocation()));
        
        super.execute();
    }
    
    private String buildPrefix() {
        
        StringBuilder str = new StringBuilder();
        
        if ((getReportUri()!=null)&&(!getReportUri().trim().equals(""))) {
            
            str.append("report-uri ");
            str.append(getReportUri());
            str.append("; ");
        }
        
        str.append("default-src 'self'");
        
        return str.toString();
    }
    
    private ContentSecurityPolicy buildContentSecurityPolicy(Map<ResourceType, Set<String>> resourceTypePolicyMap) {
        
        if ((resourceTypePolicyMap!=null)&&(!resourceTypePolicyMap.isEmpty())) {
            
            Set<ResourceType> resourceTypeSet = resourceTypePolicyMap.keySet();
            
            ContentSecurityPolicy contentSecurityPolicy = null;
            
            List<PolicyDetail> policyDetailList = null;
            
            for (ResourceType resourceType: resourceTypeSet) {
                
                Set<String> resourceTypeValues = resourceTypePolicyMap.get(resourceType);
                
                if ((resourceType!=null)&&(resourceType.getPolicy()!=null)&&(resourceTypeValues!=null)&&(!resourceTypeValues.isEmpty())) {
                 
                    if (contentSecurityPolicy==null) {
                        
                        contentSecurityPolicy = new ContentSecurityPolicy();
                        
                        PolicyHeader policyHeader = new PolicyHeader();
                        
                        if (Boolean.TRUE.toString().equalsIgnoreCase(getReportOnly())) {
                            
                            policyHeader.setKey(CONTENT_SECURITY_POLICY_REPORT_ONLY_KEY);
                            
                        } else {
                            
                            policyHeader.setKey(CONTENT_SECURITY_POLICY_KEY);
                        }
                        
                        policyHeader.setPrefix(buildPrefix());
                        
                        if (Boolean.TRUE.toString().equalsIgnoreCase(getNonce())) {
                            
                            policyHeader.setNonce(Boolean.TRUE.toString());
                            
                        } else {
                            
                            policyHeader.setNonce(Boolean.FALSE.toString());
                        }
                        
                        contentSecurityPolicy.setPolicyHeader(policyHeader);
                    }
                    
                    PolicyDetail policyDetail = buildResourceTypePolicy(resourceType, resourceTypeValues);
                    
                    if (policyDetail!=null) {
                        
                        if (policyDetailList==null) {
                            policyDetailList = new ArrayList<PolicyDetail>();
                        }
                        
                        policyDetailList.add(policyDetail);
                    }
                    
                }
            }
            
            if ((policyDetailList!=null)&&(contentSecurityPolicy!=null)) {
                
                contentSecurityPolicy.setPolicyDetailList(policyDetailList);
            }
            
            return contentSecurityPolicy;
        }
        
        return null;
    }
    
    private PolicyDetail buildResourceTypePolicy(ResourceType resourceType, Set<String> resourceTypeValues) {
        
        if ((resourceType!=null)&&(resourceTypeValues!=null)&&(!resourceTypeValues.isEmpty())) {
            
            PolicyDetail policyDetail = null;
            StringBuilder str = null;
            
            for (String resourceTypeValue: resourceTypeValues) {
                
                if ((resourceTypeValue!=null)&&(!resourceTypeValue.trim().equals(""))) {
                    
                    if (policyDetail==null) {
                        
                        policyDetail = new PolicyDetail();
                        policyDetail.setType(resourceType.getPolicy());
                        
                        str = new StringBuilder();
                        
                        if (resourceType.equals(ResourceType.SCRIPT)) {
                            
                            str.append(" 'strict-dynamic'");
                            
                        } else {
                            
                            str.append(" 'self'");
                        }
                        
                    }
                    
                    str.append(" ");
                    str.append(resourceTypeValue.trim());
                }
                
            }
            
            policyDetail.setPolicy(str.toString());
            return policyDetail;
        }
        
        
        return null;
    }
    
    private void generateContentSecurityPolicy(ContentSecurityPolicy contentSecurityPolicy, File resourceListFile) {
        
        if ((resourceListFile!=null)&&(contentSecurityPolicy!=null)) {
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db;
            
            try {
                
                db = dbf.newDocumentBuilder();
                Document doc = db.newDocument();
                Element root = doc.createElementNS(null, "contentSecurityPolicy");
                
                if (contentSecurityPolicy.getPolicyHeader()!=null) {
                    
                    Element header = doc.createElementNS(null, "policyHeader");
                    
                    Element key = doc.createElementNS(null, "key");
                    key.appendChild( doc.createCDATASection(contentSecurityPolicy.getPolicyHeader().getKey()));
                    header.appendChild(key);
                    
                    Element prefix = doc.createElementNS(null, "prefix");
                    prefix.appendChild( doc.createCDATASection(contentSecurityPolicy.getPolicyHeader().getPrefix()));
                    header.appendChild(prefix);
                    
                    Element nonce = doc.createElementNS(null, "nonce");
                    nonce.appendChild( doc.createTextNode(contentSecurityPolicy.getPolicyHeader().getNonce()) );
                    header.appendChild(nonce);
                    
                    root.appendChild( header );
                }
                
                if ((contentSecurityPolicy.getPolicyDetailList()!=null)&&(!contentSecurityPolicy.getPolicyDetailList().isEmpty())) {
                    
                    for (PolicyDetail policyDetail: contentSecurityPolicy.getPolicyDetailList()) {
                        
                        Element policyDetailElement = doc.createElementNS(null, "policyDetail");
                        
                        Element type = doc.createElementNS(null, "type");
                        type.appendChild( doc.createCDATASection(policyDetail.getType()));
                        policyDetailElement.appendChild(type);
                        
                        Element policy = doc.createElementNS(null, "policy");
                        policy.appendChild( doc.createCDATASection(policyDetail.getPolicy()));
                        policyDetailElement.appendChild(policy);
                        
                        root.appendChild( policyDetailElement );
                    }
                    
                }
                
                doc.appendChild( root );
                
                OutputStream os = Files.newOutputStream(Paths.get(resourceListFile.getParent() + "/content_security_policy.xml"), StandardOpenOption.CREATE);
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
    
    private Map<ResourceType, Set<String>> buildResourceTypePolicy(Map<ResourceType, List<ResourceTypeDetail>> resourceTypeListMap) {
        
        if ((resourceTypeListMap!=null)&&(!resourceTypeListMap.isEmpty())) {
            
            Map<ResourceType, Set<String>> resourceTypePolicyMap = null;
            
            Set<ResourceType> resourceTypeList = resourceTypeListMap.keySet();
            
            for (ResourceType resourceType: resourceTypeList) {
                
                if ((resourceType!=null)&&(resourceType.getPolicy()!=null)) {
                    
                    List<ResourceTypeDetail> resourceTypeDetailList = resourceTypeListMap.get(resourceType);
                    
                    Set<String> uniqueResourceSet = filterByUniqueResource(resourceTypeDetailList);
                    
                    if ((uniqueResourceSet!=null)&&(!uniqueResourceSet.isEmpty())) {
                        
                        if (resourceTypePolicyMap==null) {
                            resourceTypePolicyMap = new HashMap<ResourceType, Set<String>>();
                        }
                        
                        resourceTypePolicyMap.put(resourceType, uniqueResourceSet);
                    }
                    
                }
                
            }
            
            return resourceTypePolicyMap;
        }
        
        return null;
    }
    
    private Set<String> filterByUniqueResource(List<ResourceTypeDetail> resourceTypeDetailList) {
        
        if ((resourceTypeDetailList!=null)&&(!resourceTypeDetailList.isEmpty())) {
            
            Set<String> uniqueResourceSet = null;
            
            for (ResourceTypeDetail resourceTypeDetail: resourceTypeDetailList) {
                
                if ((resourceTypeDetail!=null)&&(resourceTypeDetail.getValue()!=null)&&(!resourceTypeDetail.getValue().trim().equals(""))) {
                    
                    if (uniqueResourceSet==null) {
                        uniqueResourceSet = new HashSet<String>();
                    }
                    
                    uniqueResourceSet.add(resourceTypeDetail.getValue().trim());
                    
                }
            }
            
            return uniqueResourceSet;
        }        
        
        return null;
    }
    
    private Map<ResourceType, List<ResourceTypeDetail>> buildMapFromFilteredListByResourceType(List resourceTypeList) {
        
        Map<ResourceType, List<ResourceTypeDetail>> resourceTypeListMap = null;
        
        for (ResourceType resourceType: ResourceType.values()) {
            
            List<ResourceTypeDetail> filteredResourceList = filterListByResourceType(resourceTypeList, resourceType);
            
            if ((filteredResourceList!=null)&&(!filteredResourceList.isEmpty())) {
                
                if (resourceTypeListMap==null) {
                    
                    resourceTypeListMap = new HashMap<ResourceType, List<ResourceTypeDetail>>();
                }
                
                resourceTypeListMap.put(resourceType, filteredResourceList);
            }
        }
        
        return resourceTypeListMap;
    }
    
    private List<ResourceTypeDetail> filterListByResourceType(List resourceTypeList, ResourceType resourceType) {
        
        if ((resourceTypeList!=null)&&(!resourceTypeList.isEmpty())&&(resourceType!=null)) {
            
            List<ResourceTypeDetail> filteredResourceList = null;
            
            Iterator it = resourceTypeList.iterator();
            
            while (it.hasNext()) {
                
                ResourceTypeDetail resourceTypeDetail = (ResourceTypeDetail)it.next();
                
                if ((resourceTypeDetail!=null)&&(resourceType.getName().equals(resourceTypeDetail.getName()))) {
                    
                    if (filteredResourceList==null) {
                        
                        filteredResourceList = new ArrayList<ResourceTypeDetail>();
                    }
                    
                    filteredResourceList.add(resourceTypeDetail);
                }
            }

            return filteredResourceList;
        }
        
        return null;
    }

    private Digester configureDigesterForResourceTypeList() {

        Digester digester = new Digester();
        
        digester.setValidating(false);

        digester.addObjectCreate("resourceType", ArrayList.class);

        digester.addFactoryCreate("resourceType/resourceTypeDetail", ResourceTypeDetailDigesterFactory.class);
        
        digester.addCallMethod( "resourceType/resourceTypeDetail/name", "setName", 0 );
        
        digester.addCallMethod( "resourceType/resourceTypeDetail/pattern", "setPattern", 0 );
        
        digester.addCallMethod( "resourceType/resourceTypeDetail/fileDetail", "setFileDetail", 0 );
        
        digester.addCallMethod( "resourceType/resourceTypeDetail/value", "setValue", 0 );
        
        digester.addSetNext("resourceType/resourceTypeDetail", "add");

        return digester;
    }    
    
    private Object processDigesterTask(Digester digester, File inputFile) {

        try {
            return digester.parse(inputFile);
        } catch (IOException|SAXException e) {
            throw new BuildException(e);
        }
    }  
    
    private List getResourceTypeDetailListAsObject() {

        Digester resourceTypeListConfigurationDigester = configureDigesterForResourceTypeList();

        File resourceListFile = new File(getResourceListLocation());
        
        List resourceTypeDetailList = (List) processDigesterTask(resourceTypeListConfigurationDigester, resourceListFile);

        if (resourceTypeDetailList == null) {
            
            throw new BuildException("Unable to retrieve resouce list - " + resourceListFile.getAbsolutePath());
            
        } else {
            
            return resourceTypeDetailList;
        }
    }    
    
    /**
     * @return the resourceListLocation
     */
    private String getResourceListLocation() {
        return resourceListLocation;
    }

    /**
     * @param resourceListLocation the resourceListLocation to set
     */
    public void setResourceListLocation(String resourceListLocation) {
        this.resourceListLocation = resourceListLocation;
    }

    /**
     * @return the nonce
     */
    private String getNonce() {
        return nonce;
    }

    /**
     * @param nonce the nonce to set
     */
    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    /**
     * @return the reportOnly
     */
    private String getReportOnly() {
        return reportOnly;
    }

    /**
     * @param reportOnly the reportOnly to set
     */
    public void setReportOnly(String reportOnly) {
        this.reportOnly = reportOnly;
    }

    /**
     * @return the reportUri
     */
    private String getReportUri() {
        return reportUri;
    }

    /**
     * @param reportUri the reportUri to set
     */
    public void setReportUri(String reportUri) {
        this.reportUri = reportUri;
    }

    public static void main(String[] argc ) {
        
        String resourceListLocation = "C:/eclipse/workspaces/csp-test/csp_external_resource_list.xml";
        ContentSecurityPolicyGenerator contentSecurityPolicyGenerator = new ContentSecurityPolicyGenerator();
        contentSecurityPolicyGenerator.setResourceListLocation(resourceListLocation);
        contentSecurityPolicyGenerator.execute();
    }

}
