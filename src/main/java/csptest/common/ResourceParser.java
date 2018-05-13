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
package csptest.common;

import java.util.List;

import org.apache.tools.ant.types.Resource;

/**
 * @author thealmostrealmccoy
 * 
 * - https://www.mkyong.com/java/how-to-create-xml-file-in-java-dom/
 * - http://xerces.apache.org/mirrors.cgi
 * - cat /c/xerces-2_11_0/samples/dom/DOMGenerate.java |less
 * - ant -f csp-tools.xml content_security_policy_white_list_collector -Dprojectroot=C:/eclipse/workspaces/csp-test
 * - junit-spring.xml
 * - https://stackoverflow.com/questions/3651725/match-multiline-text-using-regular-expression
 * - https://stackoverflow.com/questions/43240541/how-to-multiline-regex-but-stop-after-first-match
 * - https://examples.javacodegeeks.com/core-java/util/regex/greedy-and-non-greedy-reg-ex-matching/
 * - Content-Security-Policy: default-src 'self'; frame-src 'none'; object-src 'none‘; script-src ‘self‘ http://www.jquery.com 'nonce-EDNnf03nceIOfn39fn3e9h3sdfa‘
 * - https://github.com/cure53/XSSChallengeWiki/wiki/H5SC-Minichallenge-3:-%22Sh*t,-it%27s-CSP!%22#107-bytes
 * - https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/script-src
 * - E.g. script-src 'unsafe-inline' https: 'nonce-abcdefg' 'strict-dynamic'
 * - 14.6 Manipulating the Tag Body --- Search for FilterTag
 * - http://www.informit.com/articles/article.aspx?p=26119&seqNum=7
 * - How to load a resource from WEB-INF directory of a web archive
 * - https://stackoverflow.com/questions/1108434/how-to-load-a-resource-from-web-inf-directory-of-a-web-archive
 *  Testing
 *  Write about reporting limitation due to conflict between report-uri and report-to directive
 *  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/report-uri
 *  https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/style-src
 *  JSP Tag Libraries
 *  Write about the conflict of using strict-dynamic directive when used with whitelist resource and nonce. 
 */
public interface ResourceParser {

    public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger);
}