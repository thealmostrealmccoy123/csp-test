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
package csptest.report;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author thealmostrealmccoy
 *
 */
public class ContentSecurityPolicyViolationReporter extends HttpServlet {
    
    private static final Log log = LogFactory.getLog(ContentSecurityPolicyViolationReporter.class);
    
    @Override
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        
        String cspReport = getInputStreamContentAsString(request.getInputStream());
        
        log.debug(cspReport);
        
    }
    
    @Override
    public void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException, ServletException
    {
        doGet(request, response);
    }
    
    private String getInputStreamContentAsString(InputStream inputStream) throws IOException {
        
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
            
        }
        
        if (rawContent!=null) {

            return rawContent.toString();
            
        }
        
        return null;
    }    
    
}
