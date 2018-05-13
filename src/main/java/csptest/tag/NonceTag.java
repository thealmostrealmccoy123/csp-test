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
package csptest.tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * @author thealmostrealmccoy
 *
 */
public class NonceTag extends BodyTagSupport {
    
    private static final String INLINE_NONCE = "inlineNonce";
    
    private static final String[] PATTERN_LIST = new String[] {"<script.*?>", "<style.*?>", "<link.*?>"};
    
    public int doAfterBody() throws JspException {
        
        HttpServletRequest httpRequest = (HttpServletRequest)pageContext.getRequest();
        
        if ((httpRequest!=null)&&(httpRequest.getSession().getAttribute(INLINE_NONCE)!=null)) {

            String nonce = (String)httpRequest.getSession().getAttribute(INLINE_NONCE);
            
            BodyContent bodyContent  = getBodyContent();
            
            String bodyContentAsString = bodyContent.getString();
            
            String updatedString = applyNonceToScript(nonce, bodyContentAsString);
            
            JspWriter out = bodyContent.getEnclosingWriter();
            
            try {
                
                out.print(updatedString);
                
            } catch (IOException e) {

                //log here
            }
            
        }
        
        return (SKIP_BODY);
    }
    
    private String applyNonceToScript(String nonce, String bodyContent) {
        
        if ((nonce!=null)&&(!nonce.trim().equals(""))&&(bodyContent!=null)&&(!bodyContent.trim().equals(""))) {
            
            Map<String, String> replacementStringMap = buildreplacementStringList(nonce, bodyContent);
            
            if ((replacementStringMap!=null)&&(!replacementStringMap.isEmpty())) {
                
                StringBuilder originalContent = new StringBuilder(bodyContent.trim());
                
                replaceMatches(replacementStringMap, originalContent);
                
                if ((originalContent!=null)&&(originalContent.length()>0)) {
                    
                    return originalContent.toString();
                }
            }
        }
        
        return null;
    }
    
    private Map<String, String> buildreplacementStringList(String nonce, String bodyContent) {
        
        if ((nonce!=null)&&(!nonce.trim().equals(""))&&(bodyContent!=null)&&(!bodyContent.trim().equals(""))) {
            
            Map<String, String> replacementStringMap = null;
            
            for (String patternString: PATTERN_LIST) {
                
                Pattern pattern = Pattern.compile(patternString, Pattern.DOTALL);
                
                Matcher matcher = pattern.matcher(bodyContent);
                
                while (matcher.find()) {

                    String group = matcher.group();
                    
                    if (group != null) {
                        
                        int closingTagIndex = group.indexOf(">");
                        
                        if (closingTagIndex>=0) {
                            
                            String groupPrefix = group.substring(0, closingTagIndex);
                            
                            if (groupPrefix!=null) {
                                
                                StringBuilder replacementStringBuilder = new StringBuilder();
                                replacementStringBuilder.append(groupPrefix.trim());
                                replacementStringBuilder.append(" ");
                                replacementStringBuilder.append(nonce);
                                replacementStringBuilder.append(">");
                                
                                if (replacementStringMap==null) {
                                    replacementStringMap = new HashMap<String, String>();
                                }
                                
                                replacementStringMap.put(group, replacementStringBuilder.toString());
                            }
                            
                        }
                        
                    }
                    
                }
                
            }
            
            return replacementStringMap;
            
        }
        
        return null;
    }
    
    private void replaceMatches(Map<String, String> replacementStringMap, StringBuilder originalContent) {
        
        if ((originalContent!=null)&&(originalContent.length()>0)&&(replacementStringMap!=null)&&(!replacementStringMap.isEmpty())) {
            
            Set<String> originalStrings = replacementStringMap.keySet();
            
            if ((originalStrings!=null)&&(!originalStrings.isEmpty())) {
                
                for (String originalString: originalStrings) {
                    
                    String replacementString = replacementStringMap.get(originalString);
                    
                    if ((originalString!=null)&&(replacementString!=null)) {
                        
                        stringReplace(originalContent, originalString, replacementString);
                    }
                }
            }
            
        }
        
    }
    
    private void stringReplace(StringBuilder originalContent, String originalString, String replacementString) {
        
        int found = originalContent.indexOf(originalString);
        
        final int str1Length = originalString.length();
        
        final int str2Length = replacementString.length();
        
        while (found >= 0) {
            
            originalContent.replace(found, found + str1Length, replacementString);
            
            found = originalContent.indexOf(originalString, found + str2Length);
        }
    }
    
/* TEST CODE */
    
    class MyBodyContent extends BodyContent {
        
        private String filePath;

        /**
         * @return the filePath
         */
        private String getFilePath() {
            return filePath;
        }

        /**
         * @param filePath the filePath to set
         */
        private void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public MyBodyContent(JspWriter e) {
            super(e);
            // TODO Auto-generated constructor stub
        }
        
        public MyBodyContent(javax.servlet.jsp.JspWriter e, String filePath) {
            super(e);
            setFilePath(filePath);
        }

        @Override
        public java.io.Reader getReader() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getString() {
            
            StringBuilder rawContent = null;
            
            if (getFilePath()!=null) {
            
                try (java.io.BufferedReader bufferedReader = new java.io.BufferedReader(new java.io.FileReader(new java.io.File(getFilePath())))) {

                    int c = bufferedReader.read();
                    while (c != -1) {

                        if (rawContent == null) {
                            rawContent = new StringBuilder();
                        }
                        rawContent.append((char) c);

                        c = bufferedReader.read();
                    }
                    
                } catch (java.io.IOException e) {
                    
                }
                
            }
            
            if (rawContent!=null) {
                
                return rawContent.toString();
            }
            
            return null;
        }

        @Override
        public void writeOut(java.io.Writer arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void clear() throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void clearBuffer() throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void close() throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getRemaining() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void newLine() throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(boolean arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(char arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(int arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(long arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(float arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(double arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(char[] arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(String arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void print(Object arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println() throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(boolean arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(char arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(int arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(long arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(float arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(double arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(char[] arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(String arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void println(Object arg0) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws java.io.IOException {
            // TODO Auto-generated method stub
            
        }
        
    };
    
    private BodyContent getMyBodyContent(String filePath) {
        
        return new MyBodyContent(null, filePath);
    }
    
    private void writeChanges(String filePath, String changes) {
        
        if ((filePath!=null)&&(changes!=null)) {
            
            java.io.File file = new java.io.File(filePath);
            
            try (java.io.BufferedWriter bufferedWriter = new java.io.BufferedWriter(new java.io.FileWriter(file));) {
                
                bufferedWriter.write(changes);
                
            } catch (java.io.IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
    }
    
    public void test() {
        
        String[] filePathList = new String[] {"C:/eclipse/workspaces/csp-test/src/main/webapp/jsp/checboxradio.jsp", 
                "C:/eclipse/workspaces/csp-test/src/main/webapp/jsp/autocomplete.jsp", 
                "C:/eclipse/workspaces/csp-test/src/main/webapp/jsp/autocomplete_dom_xss.jsp",
                "C:/eclipse/workspaces/csp-test/src/main/webapp/jsp/selectmenu.jsp",
                "C:/eclipse/workspaces/csp-test/src/main/webapp/jsp/mouseclick.jsp",
                "C:/eclipse/workspaces/csp-test/src/main/webapp/jsp/carousel.jsp"};
        
        for (String filePath: filePathList) {
            
            BodyContent bodyContent = getMyBodyContent(filePath);
            
            String myBodyContent = bodyContent.getString();
            
            String updatedString = applyNonceToScript("nonce=\"abcdefg\"", myBodyContent);
            
            writeChanges(filePath + ".txt", updatedString);
            
        }
        
        
    }
    
    public static void main(String[] argc) {
        
        NonceTag nonceTag = new NonceTag();
        
        nonceTag.test();
    }
    
}
