/*
	real time subtitle translate for PotPlayer using Gemini 2.0 API
	https://ai.google.dev/api/generate-content#v1beta.models.generateContent
*/

// adjust translation accuracy or style
string default_prompt = "You are an expert subtitle translator, you can use profane language if it is present in the source, output only the translation";
// minimum milliseconds between translations, designed to keep api call rate under free tier limit
// if you have paid tier you can set it to 0
uint DefaultPause = 500;
// how much previous lines to pass to translation for improved understanding of the text
// may quickly drain token quota, increase with care
uint MaxContextLines = 50;
// gemini-2.0-flash is the default with highest quality, but limited to 15 translations per minute / 1500 per day in free tier
// gemini-2.0-flash-lite is simpler, and limited to 30 translations per minute / 1500 per day in free tier
string Model = "gemini-2.0-flash";
string Name = "Gemini-Flash-Free";

bool debug = false;

// void OnInitialize()
// void OnFinalize()
// string GetTitle() 														-> get title for UI
// string GetVersion														-> get version for manage
// string GetDesc()															-> get detail information
// string GetLoginTitle()													-> get title for login dialog
// string GetLoginDesc()													-> get desc for login dialog
// string GetUserText()														-> get user text for login dialog
// string GetPasswordText()													-> get password text for login dialog
// string ServerLogin(string User, string Pass)								-> login
// string ServerLogout()													-> logout
//------------------------------------------------------------------------------------------------
// array<string> GetSrcLangs() 												-> get source language
// array<string> GetDstLangs() 												-> get target language
// string Translate(string Text, string &in SrcLang, string &in DstLang) 	-> do translate !!

array<array<string>> LangTable =
{
  {"af", "Afrikaans"},
  {"sq", "Albanian"},
  {"am", "Amharic"},
  {"ar", "Arabic"},
  {"hy", "Armenian"},
  {"as", "Assamese"},
  {"ay", "Aymara"},
  {"az", "Azerbaijani"},
  {"bm", "Bambara"},
  {"eu", "Basque"},
  {"be", "Belarusian"},
  {"bn", "Bengali"},
  {"bho", "Bhojpuri"},
  {"bs", "Bosnian"},
  {"bg", "Bulgarian"},
  {"ca", "Catalan"},
  {"ceb", "Cebuano"},
  {"zh", "Chinese (Simplified)"},
  {"zh-TW", "Chinese (Traditional)"},
  {"co", "Corsican"},
  {"hr", "Croatian"},
  {"cs", "Czech"},
  {"da", "Danish"},
  {"dv", "Divehi"},
  {"doi", "Dogri"},
  {"nl", "Dutch"},
  {"en", "English"},
  {"eo", "Esperanto"},
  {"et", "Estonian"},
  {"ee", "Ewe"},
  {"fil", "Filipino"},
  {"fi", "Finnish"},
  {"fr", "French"},
  {"fy", "Frisian"},
  {"gl", "Galician"},
  {"ka", "Georgian"},
  {"de", "German"},
  {"el", "Greek"},
  {"gn", "Guarani"},
  {"gu", "Gujarati"},
  {"ht", "Haitian Creole"},
  {"ha", "Hausa"},
  {"haw", "Hawaiian"},
  {"he", "Hebrew"},
  {"hi", "Hindi"},
  {"hmn", "Hmong"},
  {"hu", "Hungarian"},
  {"is", "Icelandic"},
  {"ig", "Igbo"},
  {"ilo", "Ilocano"},
  {"id", "Indonesian"},
  {"ga", "Irish"},
  {"it", "Italian"},
  {"ja", "Japanese"},
  {"jv", "Javanese"},
  {"kn", "Kannada"},
  {"kk", "Kazakh"},
  {"km", "Khmer"},
  {"rw", "Kinyarwanda"},
  {"gom", "Konkani"},
  {"ko", "Korean"},
  {"kri", "Krio"},
  {"ku", "Kurdish"},
  {"ky", "Kyrgyz"},
  {"lo", "Lao"},
  {"la", "Latin"},
  {"lv", "Latvian"},
  {"ln", "Lingala"},
  {"lt", "Lithuanian"},
  {"lb", "Luxembourgish"},
  {"mk", "Macedonian"},
  {"mai", "Maithili"},
  {"mg", "Malagasy"},
  {"ms", "Malay"},
  {"ml", "Malayalam"},
  {"mt", "Maltese"},
  {"mi", "Maori"},
  {"mr", "Marathi"},
  {"mni-Mtei", "Meiteilon (Manipuri)"},
  {"mn", "Mongolian"},
  {"my", "Myanmar (Burmese)"},
  {"ne", "Nepali"},
  {"no", "Norwegian"},
  {"ny", "Nyanja (Chichewa)"},
  {"or", "Odia (Oriya)"},
  {"om", "Oromo"},
  {"ps", "Pashto"},
  {"fa", "Persian"},
  {"pl", "Polish"},
  {"pt", "Portuguese"},
  {"pa", "Punjabi"},
  {"qu", "Quechua"},
  {"ro", "Romanian"},
  {"ru", "Russian"},
  {"sm", "Samoan"},
  {"sa", "Sanskrit"},
  {"gd", "Scots Gaelic"},
  {"sr", "Serbian"},
  {"st", "Sesotho"},
  {"sn", "Shona"},
  {"sd", "Sindhi"},
  {"si", "Sinhala (Sinhalese)"},
  {"sk", "Slovak"},
  {"sl", "Slovenian"},
  {"so", "Somali"},
  {"es", "Spanish"},
  {"su", "Sundanese"},
  {"sw", "Swahili"},
  {"sv", "Swedish"},
  {"tl", "Tagalog (Filipino)"},
  {"tg", "Tajik"},
  {"ta", "Tamil"},
  {"tt", "Tatar"},
  {"te", "Telugu"},
  {"th", "Thai"},
  {"ti", "Tigrinya"},
  {"ts", "Tsonga"},
  {"tr", "Turkish"},
  {"tk", "Turkmen"},
  {"tw", "Twi (Akan)"},
  {"uk", "Ukrainian"},
  {"ur", "Urdu"},
  {"ug", "Uyghur"},
  {"uz", "Uzbek"},
  {"vi", "Vietnamese"},
  {"cy", "Welsh"},
  {"xh", "Xhosa"},
  {"yi", "Yiddish"},
  {"yo", "Yoruba"},
  {"zu", "Zulu"}
};

string UserAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari";

string GetTitle()
{
	return Name;
}

string GetVersion()
{
	return "1";
}

string GetDesc()
{
	return "<a href=\"https://github.com/OlegYch/potplayer-gemini\">Click to see documentation</a>";
}

string GetLoginTitle()
{
	return "Gemini settings";
}

string GetLoginDesc()
{
	return "Gemini prompt and API key";
}

string GetUserText()
{
	return "Prompt (optional):";
}

string GetPasswordText()
{
	return "API key:";
}

string current_prompt;
string api_key;

string ServerLogin(string User, string Pass)
{
  if (!User.empty())	current_prompt = User;
  else current_prompt = default_prompt;
	api_key = Pass;
	if (api_key.empty()) return "fail";
	return "200 ok";
}

array<string> GetSrcLangs()
{
	array<string> ret = {};
  for ( uint i = 0; i < LangTable.length(); i++)
  {
    ret.insertLast(LangTable[i][0]);
  }
	return ret;
}

array<string> GetDstLangs()
{
	return GetSrcLangs();
}

string Untranslated = "";
uint LastTime = 0;
uint Pause = DefaultPause;
array<string> ContextUser = {};
array<string> ContextModel = {};

dictionary CallGemini(string Text, string &in SrcLang, string &in DstLang, string Model)
{
  LastTime = HostGetTickCount();
  string url = "https://generativelanguage.googleapis.com/v1beta/models/" + Model + ":generateContent?key=" + api_key;
  string prompt = current_prompt;
  string context = "";
  for( uint n = 0; n < ContextUser.length(); n++ )
  {
    context += """{"role":"user", "parts":[{"text": " """ + ContextUser[n] + """ "}]},""";
    context += """{"role":"model", "parts":[{"text": " """ + ContextModel[n] + """ "}]},""";
  }
  //HostPrintUTF8(context);
  if (SrcLang.length() > 0)
  {
    prompt += ", translate from " + SrcLang + " to " + DstLang;
  } else
  {
    prompt += ", translate to " + DstLang;
  }
  string SendHeader = "Content-Type: application/json\r\n";
  string Post = """{
  "safety_settings":[
      {"category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE"},
      {"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE"},
      {"category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE"},
      {"category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE"},
      {"category": "HARM_CATEGORY_CIVIC_INTEGRITY", "threshold": "BLOCK_NONE"}
    ],
  "generation_config": {"temperature": 0.1, "seed": 100500},
  "system_instruction": { "parts": { "text": " """ + prompt + """ "}},
  "contents": [ """ +
    context +
    """{"role":"user", "parts":[{"text": " """ + Untranslated + Text + """ "}]}
  ]
  }""";
  //HostPrintUTF8(Post);

  uintptr http = HostOpenHTTP(url, UserAgent, SendHeader, Post);
  if (http != 0)
  {
		string json = HostGetContentHTTP(http);
    string headers = HostGetHeaderHTTP(http);
		HostCloseHTTP(http);
		//HostPrintUTF8(json);
		//HostPrintUTF8(headers);
    JsonReader Reader;
    JsonValue Root;
    if (Reader.parse(json, Root) && Root.isObject())
    {
      JsonValue choices = Root["candidates"];
      if (choices.isArray())
      {
        string ret = choices[0]["content"]["parts"][0]["text"].asString();
        ret.erase(ret.length() - 1, 1);
        int last = ret.findLast("\n");
        if (last > 0)
        {
          ret = ret.substr(last - 1, -1);
        }
        ContextUser.insertLast(Text);
        ret.replace("\r", "");
        ret.replace("\n", " ");
        ret.replace("\"", "'");
        ContextModel.insertLast(ret);
        ret += "\n";
        //HostPrintUTF8(ret);
        return {{'success', ret}};
      }
      else
      {
        string error = Root["error"]["message"].asString();
        HostPrintUTF8(error);
        return {{'error', error}};
      }
    }
    else
    {
      return {{'error', "Can't parse " + json}};
    }
  }
  else
  {
    return {{'error', "Can't open http connection"}};
  }
}

string Translate(string Text, string &in SrcLang, string &in DstLang)
{
  if (api_key.empty())
  {
    return "Please get an API key at https://aistudio.google.com/app/apikey and configure subtitle translation settings.";
  }
  if (debug) HostOpenConsole();
//HostPrintUTF8(Untranslated + "---");
//HostPrintUTF8(Text);
  for ( uint i = 0; i < LangTable.length(); i++)
  {
    string short = LangTable[i][0];
    string full = LangTable[i][1];
  	if (SrcLang == short) SrcLang = full;
	  if (DstLang == short) DstLang = full;
  }

  Text.replace("\"", "'");
  Text.replace("\n", " ");
  if (ContextUser.length() > MaxContextLines)
  {
    ContextUser.removeRange(0, 1);
    ContextModel.removeRange(0, 1);
  }
  //HostPrintUTF8(Text);
	string ret = "";
	uint elapsed = HostGetTickCount() - LastTime;
	if (elapsed < Pause) //add some delay between subsequent calls to hopefully fit in gemini free tier
	{
    HostPrintUTF8("Too fast, waiting for " + formatUInt(Pause - elapsed));
	  ret = "";
	} else
	{
		dictionary result = CallGemini(Text, SrcLang, DstLang, Model);
		string success = string(result['success']);
		string error = string(result['error']);
		if (!success.empty())
		{
		  ret = success;
      Untranslated = "";
      if (Pause > DefaultPause * 2)
      {
        Pause -= DefaultPause / 2; //slowly allow more requests
        HostPrintUTF8("Decreasing pause to " + formatUInt(Pause));
      }
		} else
		{
      if (Pause < DefaultPause * 10)
      {
        Pause += DefaultPause / 2;
        HostPrintUTF8("Increasing pause to " + formatUInt(Pause));
        ret = "";
      } else
      {
        ret = error; //give up and display error
      }
		}
	}
	if (ret.empty())
	{
    ret = ".";
    Untranslated += Text + " ";
    Untranslated = Untranslated.Right(1000);
	}
	SrcLang = "UTF8";
	DstLang = "UTF8";


	return ret;
}
