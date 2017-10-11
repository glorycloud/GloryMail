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
public class Email2CodePage extends CodePage {
    /**
     * Constructor for EmailCodePage.  Initializes all of the code page values.
     */
    public Email2CodePage() {
        /* Maps String to Token for the code page */
    	codepageTokens.put("UmCallerID",0x05);
    	codepageTokens.put("UmUserNotes",0x06);
    	codepageTokens.put("UmAttDuration",0x07);
    	codepageTokens.put("UmAttOrder",0x08);
    	codepageTokens.put("ConversationId",0x09);
    	codepageTokens.put("ConversationIndex",0x0A);
    	codepageTokens.put("LastVerbExecuted",0x0B);
    	codepageTokens.put("LastVerbExecutionTime",0x0C);
    	codepageTokens.put("ReceivedAsBcc",0x0D);
    	codepageTokens.put("Sender",0x0E);
    	codepageTokens.put("CalendarType",0x0F);
    	codepageTokens.put("IsLeapMonth",0x10);
    	codepageTokens.put("AccountId",0x11);
    	codepageTokens.put("FirstDayOfWeek",0x12);
    	codepageTokens.put("MeetingMessageType",0x13);

        /* Maps token to string for the code page */
        for (String s : codepageTokens.keySet()) {
            codepageStrings.put(codepageTokens.get(s), s);
        }

        codePageIndex = 22;
        codePageName = "Email2";
    }
}
