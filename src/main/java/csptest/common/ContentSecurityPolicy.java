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

import java.util.ArrayList;
import java.util.List;

/**
 * @author thealmostrealmccoy
 *
 */
public class ContentSecurityPolicy {
    
    private PolicyHeader policyHeader;
    
    private List<PolicyDetail> policyDetailList;

    /**
     * @return the policyHeader
     */
    public PolicyHeader getPolicyHeader() {
        return policyHeader;
    }

    /**
     * @param policyHeader the policyHeader to set
     */
    public void setPolicyHeader(PolicyHeader policyHeader) {
        this.policyHeader = policyHeader;
    }

    /**
     * @return the policyDetailList
     */
    public List<PolicyDetail> getPolicyDetailList() {
        return policyDetailList;
    }

    /**
     * @param policyDetailList the policyDetailList to set
     */
    public void setPolicyDetailList(List<PolicyDetail> policyDetailList) {
        this.policyDetailList = policyDetailList;
    }
    
    public void addPolicyDetail(PolicyDetail policyDetail) {
        
        if (policyDetail!=null) {
            
            if (policyDetailList==null) {
                policyDetailList = new ArrayList<PolicyDetail>();
            }
            
            policyDetailList.add(policyDetail);
        }
        
    }
    

}