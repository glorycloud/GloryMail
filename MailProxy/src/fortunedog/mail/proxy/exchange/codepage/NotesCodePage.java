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
public class NotesCodePage extends CodePage {
    /**
     * Constructor for EmailCodePage.  Initializes all of the code page values.
     */
    public NotesCodePage() {
        /* Maps String to Token for the code page */
    	codepageTokens.put("Subject",0x05);
    	codepageTokens.put("MessageClass",0x06);
    	codepageTokens.put("LastModifiedDate",0x07);
    	codepageTokens.put("Categories",0x08);
    	codepageTokens.put("Category",0x09);
        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 23;
        codePageName = "Notes";
    }
}
