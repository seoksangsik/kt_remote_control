'use strict';

acba.define('js:Config', ['log:kids(Config)', 'js:Enum'], 
function (log, Enum) {

    log.level = log.DEBUG;

    var isLive = true,
        URL = Enum.URL,
        usbProperty = {};

    (function () {
    var xhr = new XMLHttpRequest(),
        responseText = undefined;

    xhr.open('GET', URL.PROPERTIES, false);
    xhr.send();
    responseText = xhr.responseText;
    if (responseText && responseText !== '') {
        log.debug('get usb1 webmw.properties : \'' + responseText + '\'');

        var i = 0,
            line, key, value,
            propArray = responseText.split('\n'),
            lineArray;

        for (i = 0; i < propArray.length; i++) {
            line = propArray[i];

            if (line) {
                log.debug('line[' + i + ']=' + line);
                if (line === '' || line.indexOf('#') === 0) {
                    log.error('skip line');
                } else {
                    lineArray = line.split('=');
                    if (lineArray) {
                        if (lineArray.length === 2) {
                            key = lineArray[0];
                            value = lineArray[1];
    
                            if (key && value) {
                                log.debug('add key=' + key + ', value=' + value);
                                usbProperty[key] = value;
                            }
                        } else {
                            log.error('invalid, lineArray.length=' + lineArray.length);
                        }
                    } else {
                        log.error('invalid, lineArray=' + lineArray);
                    }
                }
            } else {
                log.error('invalid, line[' + i + ']=' + line);
            }
        }

        if (usbProperty['kidscare.live']) {
            isLive = usbProperty['kidscare.live'] === 'true';
            log.debug('usb1 web.properties, kidscare.live=' + usbProperty['kidscare.live'] + ', isLive='+ isLive);
        } else if (usbProperty['kidscare.live.server']) {
            isLive = usbProperty['kidscare.live.server'] === 'true';
            log.debug('usb1 web.properties, kidscare.live.server=' + usbProperty['kidscare.live.server'] + ', isLive='+ isLive);
        }
    } else {
        log.debug('doesn\'t exist usb1 web.properties.');
    }
}());

    return {
       getControlURL : function() {
          var url = usbProperty['kidscare.control.address'];

           if (url) {
               log.debug('getControlURL, usb url=\'' + url + '\'');
           } else {
               url = isLive ? URL.CONTROL.LIVE : URL.CONTROL.TEST;
           }
           log.debug('getControlURL, isLive=' + isLive + ', url=\'' + url + '\'');
           return url;
       },
       getHDSURL : function() {
           var url = usbProperty['kidscare.hds.address'];

           if (url) {
               log.debug('getHDSURL, usb url=\'' + url + '\'');
           } else {
               url = isLive ? URL.HDS.LIVE : URL.HDS.TEST;
           }
           log.debug('getHDSURL, isLive=' + isLive + ', url=\'' + url + '\'');
           return url;
       },
       getKakaoURL : function() {
           var url = usbProperty['kidscare.kakao.address'];

           if (url) {
               log.debug('getKakaoURL, usb url=\'' + url + '\'');
           } else {
               url = URL.KAKAO; 
           }
           log.debug('getKakaoURL, url=\'' + url + '\'');
           return url;
       },
       getAMOCURL : function() {
            var url = URL.AMOC;
            log.debug('getAMOCURL, url=\'' + url + '\'');
            return url;
       },
    };
});