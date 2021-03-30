'use strict';

acba.define('js:VBOWrapper',
    ['log:kids(VBOWrapper)', 'js:nav/STBInfo'],
    function (log, stbInfo) {

    KIDS.register(log);

    var vbo = window.oipfObjectFactory.createVideoBroadcastObject(),
        cconfig = undefined,
        STBInfo = stbInfo['STBInfo'],
        favList;

    vbo.setAttribute("class", "VideoBroadcast");
    document.body.appendChild(vbo);
    cconfig = vbo.getChannelConfig();

    function getChannelList() {
        var i = 0,
            channelList = [],
            skyKidsChannels, ktKidsChannels,
            kidsCareMode = (STBInfo.getInfo('kidsCareMode') == 'true') ? true : false;

        if (kidsCareMode) {
            log.debug("getChannelList, KidsCare Channel List!");
            channelList = [];
            favList = cconfig.favouriteLists;
            skyKidsChannels = favList.getFavouriteList("favourite:SKYLIFE_CHANNELS_KIDS_CARE");
            ktKidsChannels = favList.getFavouriteList("favourite:KT_CHANNELS_KIDS_CARE");

            log.debug("getChannelList, KidsCare Count, Skylife=" + skyKidsChannels.length + ", KT=" + ktKidsChannels.length);
            for (i = 0; i < skyKidsChannels.length; i++) {
                channelList.push(skyKidsChannels[i]);
            }
            for (i = 0; i < ktKidsChannels.length; i++) {
                channelList.push(ktKidsChannels[i]);
            }
        } else {
            log.debug("getChannelList, All Channel List!");
            channelList = cconfig.channelList;
            log.debug("getChannelList, All Channel Count=" + channelList.length);
        }
        return channelList;
    }

    function searchChannel(value) {
        var list = getChannelList(),
            len = list.length,
            channel,
            idx;

        for (idx = 0; idx < len; idx++) {
            channel = list[idx];
            if (channel.majorChannel == value || channel.name == value) {
                return channel;
            }
        }
        return null;
    }

    return {
        setChannel: function (channel) {
            log.debug("setChannel, channel=" + channel);
            if (channel === undefined || channel === null) {
                return;
            }

            vbo.setChannel(channel);
        },
        setChannelByServiceID: function (value) {
            log.debug('setChannelByServiceID, value=' + value);
            var ch = searchChannel(value);
            if (ch) {
                vbo.setChannel(ch);
            } else {
                log.error('setChannelByServiceID, don\'t find channel(' + value + ')');
            }
        },
        setAllChannel: function (ccid) {
            log.debug("setAllChannel, ccid: " + ccid);
            if (ccid === undefined || ccid === null) {
                return;
            }
            vbo.setChannel(cconfig.channelList.getChannel(ccid));
        },
    };
});