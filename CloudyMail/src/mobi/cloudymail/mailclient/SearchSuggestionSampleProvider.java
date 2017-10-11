package mobi.cloudymail.mailclient;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestionSampleProvider extends
		SearchRecentSuggestionsProvider {

	final static String AUTHORITY="mobi.cloudymail.mailclient.SearchSuggestionSampleProvider";
	final static int MODE=DATABASE_MODE_QUERIES;
	
	public SearchSuggestionSampleProvider(){
		super();
		setupSuggestions(AUTHORITY, MODE);
	}
}
