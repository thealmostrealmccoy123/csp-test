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
public enum ResourceType {
    
    SCRIPT("script", "<script.*?</script>", "script-src"), ANCHOR("anchor", "<a.*?</a>"), IMAGE("image", "<img.*?>", "img-src"), FRAME("frame", "<iframe.*?</iframe>", "frame-src"), LINK("link", "<link.*?>", "manifest-src"), STYLE("style", "<link.*?>", "style-src");
    
    private String name;
    
    private String pattern;
    
    private String policy;
    
    ResourceType(String name) {
        setName(name);
    }
    
    ResourceType(String name, String pattern) {
        setName(name);
        setPattern(pattern);
    }    

    ResourceType(String name, String pattern, String policy) {
        setName(name);
        setPattern(pattern);
        setPolicy(policy);
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    private void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    private void setName(String name) {
        this.name = name;
    }

    /**
     * @return the policy
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * @param policy the policy to set
     */
    private void setPolicy(String policy) {
        this.policy = policy;
    }
    
}