/* Copyright 2010 Matthew Brace
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fortunedog.mail.proxy.exchange.codepage;


/**
 * This class is the specific code page for Emails in the ActiveSync protocol.
 * The code page number is 2.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class RightsManagementCodePage extends CodePage {
    /**
     * Constructor for EmailCodePage.  Initializes all of the code page values.
     */
    public RightsManagementCodePage() {
        /* Maps String to Token for the code page */
    	codepageTokens.put("RightsManagementSupport",0x05);
    	codepageTokens.put("RightsManagementTemplates",0x06);
    	codepageTokens.put("RightsManagementTemplate",0x07);
    	codepageTokens.put("RightsManagementLicense",0x08);
    	codepageTokens.put("EditAllowed",0x09);
    	codepageTokens.put("ReplyAllowed",0x0A);
    	codepageTokens.put("ReplyAllAllowed",0x0B);
    	codepageTokens.put("ForwardAllowed",0x0C);
    	codepageTokens.put("ModifyRecipientsAllowed",0x0D);
    	codepageTokens.put("ExtractAllowed",0x0E);
    	codepageTokens.put("PrintAllowed",0x0F);
    	codepageTokens.put("ExportAllowed",0x10);
    	codepageTokens.put("ProgrammaticAccessAllowed",0x11);
    	codepageTokens.put("RMOwner",0x12);
    	codepageTokens.put("ContentExpiryDate",0x13);
    	codepageTokens.put("TemplateID",0x14);
    	codepageTokens.put("TemplateName",0x15);
    	codepageTokens.put("TemplateDescription",0x16);
    	codepageTokens.put("ContentOwner",0x17);
    	codepageTokens.put("RemoveRightsManagementDistribution",0x18);
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 24;
        codePageName = "Notes";
    }
}
