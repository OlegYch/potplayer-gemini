# potplay-gemini

Uses [Gemini](https://gemini.google.com/app) to translate subtitles in real time to and from any of >100 supported languages in [PotPlayer](https://potplayer.daum.net/)

## Installation
- Go to https://aistudio.google.com/app/api-keys and register to get Gemini key
  ![API Key](images/key.png)
- Download zip file from [releases](https://github.com/OlegYch/potplayer-gemini/releases)
- Unzip to PotPlayer installation folder, eg `C:\Program Files\PotPlayer\Extension\Subtitle\Translate`
- Launch PotPlayer
- Open Settings -> Extensions -> Subtitle Translation:
![Preferences](images/prefs.png)
- Select Gemini and click Login...
- Copy the key from first step into 'API Key' field
![Login](images/login.png)
- Done, enjoy any video with subtitles translated to your language of choice
                              

## Tuning
### Prompt
During extension login you can provide custom translation prompt.
![Login](images/login.png)

This can be used to adjust translation accuracy or style. You can see the default prompt [here](src/main/as/gemini.as#L7), use it as a sample.

