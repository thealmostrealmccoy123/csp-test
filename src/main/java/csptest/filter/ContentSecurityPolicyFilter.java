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
package csptest.filter;

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import csptest.common.ContentSecurityPolicy;
import csptest.common.PolicyDetail;
import csptest.common.PolicyHeader;

/**
 * @author thealmostrealmccoy
 *
 */
public class ContentSecurityPolicyFilter implements Filter {
    
    private static final String POLICY_LOCATION = "policyLocation";
    
    private static final String NONCE_CHARACTER_SET = "zxcvbnmasdfghjkqwertyuiop23456789ZXCVBNMASDFGHJKLQWERTYUP";
    
    private static final String NONCE_GENERATOR_ALGORITHM = "SHA1PRNG";
    
    private static final String NONCE_GENERATOR_ALGORITHM_PROVIDER = "SUN";
    
    private static final String CONTENT_SECURITY_POLICY_KEY = "contentSecurityPolicyKey123";
    
    private static final String CONTENT_SECURITY_POLICY_VALUE = "contentSecurityPolicyValue123";
    
    private static final String POLICY_HEADER_NONCE = "policyHeaderNonce";
    
    private static final String INLINE_NONCE = "inlineNonce";
    
    private static final String[] NONCE_POLICY_TYPE = new String[] {"script-src", "style-src"};
    
    private ContentSecurityPolicy contentSecurityPolicy;
    
    private String generateNonce() throws NoSuchProviderException, NoSuchAlgorithmException {
        
        StringBuilder nonce = new StringBuilder();
        
        SecureRandom generator = SecureRandom.getInstance(NONCE_GENERATOR_ALGORITHM, NONCE_GENERATOR_ALGORITHM_PROVIDER);
        
        for(int count = 0; count < 32; count++)
        {
            int temp = generator.nextInt(NONCE_CHARACTER_SET.length());
            nonce.append(NONCE_CHARACTER_SET.charAt(temp));
        }

        return nonce.toString();
        
    }
    
    private boolean isNoncePolicyType(String type) {
        
        if ((type!=null)&&(!type.trim().equals(""))) {
            
            for (String policyType:NONCE_POLICY_TYPE) {
                
                if (policyType.equals(type)) {
                    
                    return true;
                }
            }
            
        }
        
        return false;
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest)request;
        
        if ((httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_KEY)!=null)&&(httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_VALUE)!=null)) {
            
            HttpServletResponse httpResponse = (HttpServletResponse)response;
            
            String contentSecurityPolicyKey = (String)httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_KEY);
            
            String contentSecurityPolicyValue = (String)httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_VALUE);
            
            httpResponse.setHeader(contentSecurityPolicyKey, contentSecurityPolicyValue);
            
        } else if ((getContentSecurityPolicy()!=null)&&(getContentSecurityPolicy().getPolicyHeader()!=null)&&(getContentSecurityPolicy().getPolicyHeader().getKey()!=null)&&(!getContentSecurityPolicy().getPolicyHeader().getKey().trim().equals(""))&&(getContentSecurityPolicy().getPolicyHeader().getPrefix()!=null)&&(!getContentSecurityPolicy().getPolicyHeader().getPrefix().trim().equals(""))) {
            
                String contentSecurityPolicyKey = getContentSecurityPolicy().getPolicyHeader().getKey();
                
                httpRequest.getSession().setAttribute(CONTENT_SECURITY_POLICY_KEY, contentSecurityPolicyKey);
                
                String contentSecurityPolicyValue = buildContentSecurityPolicyHeaderValue(getContentSecurityPolicy(), httpRequest);
                
                if ((contentSecurityPolicyValue!=null)&&(!contentSecurityPolicyValue.trim().equals(""))) {
                    
                    httpRequest.getSession().setAttribute(CONTENT_SECURITY_POLICY_VALUE, contentSecurityPolicyValue);
                }
                
                if ((httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_KEY)!=null)&&(httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_VALUE)!=null)) {
                    
                    HttpServletResponse httpResponse = (HttpServletResponse)response;
                    httpResponse.setHeader((String)httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_KEY), (String)httpRequest.getSession().getAttribute(CONTENT_SECURITY_POLICY_VALUE));
                }
        }

        chain.doFilter(request, response);
    }
    
    private String buildContentSecurityPolicyHeaderValue(ContentSecurityPolicy contentSecurityPolicy, HttpServletRequest request) {
        
        if ((contentSecurityPolicy!=null)&&(contentSecurityPolicy.getPolicyHeader()!=null)&&(contentSecurityPolicy.getPolicyHeader().getPrefix()!=null)&&(!contentSecurityPolicy.getPolicyHeader().getPrefix().trim().equals(""))) {
            
            StringBuilder str = new StringBuilder();
            str.append(contentSecurityPolicy.getPolicyHeader().getPrefix().trim());
            
            if ((contentSecurityPolicy.getPolicyDetailList()!=null)&&(!contentSecurityPolicy.getPolicyDetailList().isEmpty())) {
                
                for (PolicyDetail policyDetail: contentSecurityPolicy.getPolicyDetailList()) {
                    
                    if ((policyDetail!=null)&&(isNoncePolicyType(policyDetail.getType()))) {
                        
                        String policyTypeHeaderValue = buildContentSecurityPolicyHeaderValueForScriptTypeAndNonce(policyDetail, Boolean.TRUE.toString().equals(contentSecurityPolicy.getPolicyHeader().getNonce()), request);
                        
                        if (policyTypeHeaderValue!=null) {
                            
                            str.append("; ");
                            str.append(policyTypeHeaderValue);
                        }
                        
                    } else if ((policyDetail!=null)&&(policyDetail.getType()!=null)&&(!policyDetail.getType().trim().equals(""))&&(policyDetail.getPolicy()!=null)&&(!policyDetail.getPolicy().trim().equals(""))) {
                        
                        str.append("; ");
                        str.append(policyDetail.getType());
                        str.append(" ");
                        str.append(policyDetail.getPolicy().trim());
                    }
                    
                }
            }
            
            return str.toString();
        }
        
        return null;
    }
    
    private String buildContentSecurityPolicyHeaderValueForScriptTypeAndNonce(PolicyDetail policyDetail, boolean addNonce, HttpServletRequest request) {
        
        if ((policyDetail!=null)&&(policyDetail.getPolicy()!=null)&&(!policyDetail.getPolicy().trim().equals(""))) {
            
            StringBuilder str = new StringBuilder();
            str.append(policyDetail.getType());
            str.append(" ");
            str.append(policyDetail.getPolicy().trim());
             
            if ((addNonce)&&(request!=null)) {
                
                String policyHeaderNonce = null;
                
                if ((request.getSession().getAttribute(POLICY_HEADER_NONCE)!=null)&&(request.getSession().getAttribute(INLINE_NONCE)!=null)) {
                    
                    policyHeaderNonce = (String)request.getSession().getAttribute(POLICY_HEADER_NONCE);
                    
                } else {
                    
                    try {
                        
                        String nonce = generateNonce();
                        
                        if ((nonce!=null)&&(!nonce.trim().equals(""))) {
                            
                            policyHeaderNonce = "'nonce-" + nonce.trim() + "'";
                            
                            request.getSession().setAttribute(POLICY_HEADER_NONCE, policyHeaderNonce);
                            
                            String inlineNonce = "nonce=\"" + nonce.trim()  + "\"";
                            
                            request.getSession().setAttribute(INLINE_NONCE, inlineNonce);
                        }
                        
                        
                    } catch (NoSuchProviderException | NoSuchAlgorithmException e) {
                        
                        //log here
                    }
                    
                }
                
                if ((policyHeaderNonce!=null)&&(!policyHeaderNonce.trim().equals(""))) {
                    
                    str.append(" ");
                    str.append(policyHeaderNonce);
                }
                
            }
            
            return str.toString();
        }
        
        return null;
    }
    
 

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        String policyLocation = filterConfig.getInitParameter(POLICY_LOCATION);
        
        if ((policyLocation!=null)&&(!policyLocation.trim().equals(""))) {
            
            //E.g. servletContext.getResourceAsStream("/WEB-INF/myfile");
            InputStream policyStream = filterConfig.getServletContext().getResourceAsStream(policyLocation);
            
            ContentSecurityPolicy contentSecurityPolicy = getGeneratedContentSecurityPolicy(policyStream);
            
            setContentSecurityPolicy(contentSecurityPolicy);
        }
        
    }
    
    private ContentSecurityPolicy getGeneratedContentSecurityPolicy(InputStream policyStream) {
        
        if (policyStream!=null) {
            
            Digester contentSecurityPolicyDigester = configureDigesterForResourceTypeList();

            ContentSecurityPolicy contentSecurityPolicy = (ContentSecurityPolicy) processDigesterTask(contentSecurityPolicyDigester, policyStream);
            
            return contentSecurityPolicy;
        }
        
        return null;
    }
    
    private Digester configureDigesterForResourceTypeList() {

        Digester digester = new Digester();
        
        digester.setValidating(false);

        digester.addObjectCreate("contentSecurityPolicy", ContentSecurityPolicy.class);
        
        digester.addObjectCreate("contentSecurityPolicy/policyHeader", PolicyHeader.class);
        
        digester.addCallMethod( "contentSecurityPolicy/policyHeader/key", "setKey", 0 );        

        digester.addCallMethod( "contentSecurityPolicy/policyHeader/prefix", "setPrefix", 0 );
        
        digester.addCallMethod( "contentSecurityPolicy/policyHeader/nonce", "setNonce", 0 );
        
        digester.addSetNext("contentSecurityPolicy/policyHeader", "setPolicyHeader");

        digester.addObjectCreate("contentSecurityPolicy/policyDetail", PolicyDetail.class);
        
        digester.addCallMethod( "contentSecurityPolicy/policyDetail/type", "setType", 0 );
        
        digester.addCallMethod( "contentSecurityPolicy/policyDetail/policy", "setPolicy", 0 );        
        
        digester.addSetNext("contentSecurityPolicy/policyDetail", "addPolicyDetail");
        
        return digester;
    }
    
    private Object processDigesterTask(Digester digester, InputStream policyStream) {

        try {
            
            return digester.parse(policyStream);
            
        } catch (IOException|SAXException e) {
            
            //log here
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the contentSecurityPolicy
     */
    private ContentSecurityPolicy getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    /**
     * @param contentSecurityPolicy the contentSecurityPolicy to set
     */
    private void setContentSecurityPolicy(ContentSecurityPolicy contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }
    
    /* TEST CODE */    
    
    public void test() {
        
        java.io.File file = new java.io.File("C:/eclipse/workspaces/csp-test/src/main/webapp/content_security_policy.xml");
        try {
            
            InputStream policyStream = new java.io.FileInputStream(file);
            
            ContentSecurityPolicy contentSecurityPolicy = getGeneratedContentSecurityPolicy(policyStream);
            
            if ((contentSecurityPolicy!=null)&&(contentSecurityPolicy.getPolicyHeader()!=null)&&(contentSecurityPolicy.getPolicyHeader().getKey()!=null)&&(!contentSecurityPolicy.getPolicyHeader().getKey().trim().equals(""))) {
                
                String contentSecurityPolicykey = contentSecurityPolicy.getPolicyHeader().getKey().trim(); 
                
                String contentSecurityPolicyValue = buildContentSecurityPolicyHeaderValue(contentSecurityPolicy, null);
                
                System.out.println(contentSecurityPolicykey + ":" + contentSecurityPolicyValue);
            }
            
        } catch (java.io.FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
                
    }
    
    public static void main(String[] argc) {
        
        ContentSecurityPolicyFilter contentSecurityPolicyFilter = new ContentSecurityPolicyFilter();
        
        contentSecurityPolicyFilter.test();
    }

}