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

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.ObjectCreationFactory;
import org.xml.sax.Attributes;

/**
 * @author thealmostrealmccoy
 *
 */
public class ResourceTypeDetailDigesterFactory implements ObjectCreationFactory {
    
    private Digester digester;

    /* (non-Javadoc)
     * @see org.apache.commons.digester.ObjectCreationFactory#createObject(org.xml.sax.Attributes)
     */
    @Override
    public Object createObject(Attributes arg0) throws Exception {
        return ContentSecurityPolicyFactory.INSTANCE.getResourceTypeDetailInstance();
    }

    /* (non-Javadoc)
     * @see org.apache.commons.digester.ObjectCreationFactory#getDigester()
     */
    @Override
    public Digester getDigester() {
        return digester;
    }

    /* (non-Javadoc)
     * @see org.apache.commons.digester.ObjectCreationFactory#setDigester(org.apache.commons.digester.Digester)
     */
    @Override
    public void setDigester(Digester digester) {
        this.digester = digester;
    }

}