/*
	real time subtitle translate for PotPlayer using Gemini 2.0 API
	https://ai.google.dev/api/generate-content#v1beta.models.generateContent
*/

// adjust translation accuracy or style
string default_prompt = "You are an expert subtitle translator, you can use profane language if it is present in the source, output only the translation";
// #replaced by build
string Name = "Gemini";

// #replaced by build
bool debug = false;

// #replaced by build
array<string> libs = {};

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
	return "2";
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
string api_keys;

string ServerLogin(string User, string Pass)
{
  if (!User.empty())	current_prompt = User;
  else current_prompt = default_prompt;
	api_keys = Pass;
	if (api_keys.empty()) return "Empty API key";
  string result = Translate("Hello", "English", "French");
  if (!result.empty())
  {
  	return "200 ok";
  }
  else
  {
    return result;
  }
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

uintptr translate;
string Translate(string Text, string &in SrcLang, string &in DstLang)
{
  if (debug) HostOpenConsole();
  if (api_keys.empty())
  {
    return "Please get an API key at https://aistudio.google.com/app/apikey and configure subtitle translation settings.";
  }
  for ( uint i = 0; i < LangTable.length(); i++)
  {
    string short = LangTable[i][0];
    string full = LangTable[i][1];
  	if (SrcLang == short) SrcLang = full;
	  if (DstLang == short) DstLang = full;
  }
  if (translate == 0)
  {
    uintptr lib;
    for ( uint i = 0; i < libs.length(); i++)
    {
      lib = HostLoadLibrary(libs[i]);
      HostPrintUTF8(libs[i] + ": " + formatUInt(lib));
    }
    translate = HostGetProcAddress(lib, "translate");
    HostPrintUTF8("translate: " + formatUInt(translate));
  }
  uintptr res = HostCallProcUIntPtr(translate, "ppppp", HostString2UIntPtr(Text), HostString2UIntPtr(current_prompt), HostString2UIntPtr(SrcLang), HostString2UIntPtr(DstLang), HostString2UIntPtr(api_keys));
	SrcLang = "UTF8";
	DstLang = "UTF8";
	return HostUIntPtr2String(res);
}
