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

/**
 * @author thealmostrealmccoy
 *
 */
public enum ContentSecurityPolicyFactory {
    
    INSTANCE;
    
    public ResourceTypeDetail getResourceTypeDetailInstance() {
        
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
    
}
