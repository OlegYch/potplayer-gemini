# potplay-gemini

Uses [Gemini](https://gemini.google.com/app) to translate subtitles in real time to and from any of >100 supported languages in [PotPlayer](https://potplayer.daum.net/)

## Installation
- Go to https://aistudio.google.com/app/apikey and register to get Gemini key (if you haven't already)
  ![API Key](images/key.png)
- Download zip file from [releases](/releases)
- Unzip to PotPlayer installation folder, eg `C:\Program Files\PotPlayer\Extension\Subtitle\Translate`
- Launch PotPlayer
- Open Settings -> Extensions -> Subtitle Translation:
![Preferences](images/prefs.png)
- Select one of Gemini Engines and click Login...
- Copy the key from first step into 'API Key' field
![Login](images/login.png)
- Adjust PotPlayer translation settings in context menu if necessary
![Context menu](images/context.png)
- Done, enjoy any video with subtitles translated to your language of choice
                              

## Tuning
### Models
At the moment there are three versions of extension with different default settings:
- Gemini-Flash-Free uses the best available model for translation and is tuned to be used with free account, by forcing a bit of delay between translation calls.
- Gemini-Flash-Paid uses the same model and a bit more relaxed settings for better experience, but can be problematic when using with free account. Try it after you set up Gemini billing. 
- Gemini-Lite-Free uses a simpler, faster model which is suitable for many languages, but not all. Try it if you don't want to set up Gemini billing and get too much translation delay.

Note that free Gemini plan is limited to 1500 translations per day, which should probably be enough for one movie.

### Prompt
During extension login you can provide custom translation prompt.
![Login](images/login.png)
This can be used to adjust translation accuracy or style. You can see the default prompt [here](src/main/as/gemini.as#L7), use it as a sample.

### Fine tuning
A few more settings are available at the start of `*.as` files, you can edit them freely, but don't forget to restart PotPlayer.