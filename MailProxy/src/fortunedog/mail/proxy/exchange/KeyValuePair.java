/* Copyright 2010 Vivek Iyer
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

package fortunedog.mail.proxy.exchange;

/**
 * @author Vivek Iyer
 *
 * Helper class that stores contact details as KeyValuePairs
 */
public class KeyValuePair {
	public enum Type{
		MOBILE,
		PHONE,
		EMAIL,
		OTHER
	}
	private Type _type;
	
	public Type get_type() {
		return _type;
	}
	public void set_type(Type _type) {
		this._type = _type;
	}
	
	private String _key;
	
	public String getKey() {
		return _key;
	}
	public void setKey(String key) {
		_key = key;
	}
	
	private String _value;

	public String getValue() {
		return _value;
	}
	public void setValue(String value) {
		_value = value;
	}
	
	public KeyValuePair(String key, String value){
		_key = key;
		_value = value;
	}
	public String toString(){
		return("Key="+_key+"\nValue="+_value);
	}
}
