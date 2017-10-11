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
 * This class is the specific code page for ResolveRecipients in the ActiveSync protocol.
 * The code page number is 10.
 * 
 * @version 1.0
 * @author  Matthew Brace
 */
public class ResolveRecipientsCodePage extends CodePage {
    /**
     * Constructor for ResolveRecipientsCodePage.  Initializes all of the code page values.
     */
    public ResolveRecipientsCodePage() {
        /* Maps String to Token for the code page */
        codepageTokens.put("ResolveRecipients", 0x05);
        codepageTokens.put("Response", 0x06);
        codepageTokens.put("Status", 0x07);
        codepageTokens.put("Type", 0x08);
        codepageTokens.put("Recipient", 0x09);
        codepageTokens.put("DisplayName", 0x0a);
        codepageTokens.put("EmailAddress", 0x0b);
        codepageTokens.put("Certificates", 0x0c);
        codepageTokens.put("Certificate", 0x0d);
        codepageTokens.put("MiniCertificate", 0x0e);
        codepageTokens.put("Options", 0x0f);
        codepageTokens.put("To", 0x10);
        codepageTokens.put("CertificateRetrieval", 0x11);
        codepageTokens.put("RecipientCount", 0x12);
        codepageTokens.put("MaxCertificates", 0x13);
        codepageTokens.put("MaxAmbiguousRecipients", 0x14);
        codepageTokens.put("CertificateCount", 0x15);
        codepageTokens.put("Availability",  0x16);
        codepageTokens.put("StartTime", 0x17);
        codepageTokens.put("EndTime", 0x18);
        codepageTokens.put("MergedFreeBusy", 0x19);
        codepageTokens.put("Picture", 0x1A);
        codepageTokens.put("MaxSize", 0x1B);
        codepageTokens.put("Data", 0x1C);
        codepageTokens.put("MaxPictures", 0x1D);        
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 0x0a;
        codePageName = "ResolveRecipients";
    }
}
