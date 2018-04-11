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
 *
 */
public interface ResourceParser {

    public List<ResourceTypeDetail> parseResource(String fileContent, Resource resource, String baseDirectory, StringBuilder logger);
}