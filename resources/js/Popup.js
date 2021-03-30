'use strict';

acba.define('js:Popup', ['log:kids(Popup)', 'js:nav_util', 'js:Enum',
                         'js:Config', 'js:network', 'js:NavCoreProxy'], 
function(log, util, Enum, config, network, navCoreProxy) {
    log.level = log.DEBUG;

    var popup = {},
        am = window.oipfObjectFactory.createApplicationManagerObject(),
        own = am.getOwnerApplication(window.document),
        mouseControl = oipfObjectFactory.createMouseControlObject(),
        ajax = network.Ajax.getInstance(),
        $ = util.NavUtil,
        layoutDiv = $.id('layout'),
        textWidthDiv = $.id('text_width'),
        popupType = -1,
        kt_keycodes = acba('js:keycodes'),
        allKey = [],
        ERROR_CODE = Enum.ERROR_CODE,
        VALUE = Enum.VALUE,
        KEY = Enum.KEY,
        POPUP = Enum.POPUP,
        CHAR = Enum.CHAR;

        for (var index = 0; index < kt_keycodes.length; index++) {
    //            log.debug('get keycode of ' + kt_keycodes[index].name + '(' + kt_keycodes[index].code + ')');
            allKey.push(kt_keycodes[index].code);
        }

    var parameter = {},
        focusManager = null,
        kidsMethod = null,
        canvasElement = null; // for reply emoticon popup

    function setKidsMethod(action) {
        log.debug('setKidsMethod, action=' + action);
        kidsMethod = action;
    }

    function isDataChannel() {
        var realCurrentChannel = navCoreProxy.getRealCurrentChannel(),
            isDataChannelType = realCurrentChannel
                                && (realCurrentChannel.channelType === Channel.TYPE_OTHER); // Channel.TYPE_OTHER=2
        log.debug('isDataChannel=' + isDataChannelType);
        return isDataChannelType;
    }

    function boundApp() {
        var boundApps = am.findApplications('useType', 'bound'),
            boundApp = boundApps && boundApps.length > 0;
        log.debug('boundApp=' + boundApp
                + (boundApp ? ', app\'s count : ' + boundApps.length : ''));
        return boundApp;
    }

    function setVisible(visible, activate) {
        log.debug('setVisible, visible=' + visible + ', activate=' + activate);

        if (visible) {
            if (activate && (isDataChannel() || boundApp())) {
                log.debug('selectPromoChannel!');
                navCoreProxy.selectPromoChannel();
            }

            own.privateData.keyset.setValue(own.privateData.keyset.OTHER, allKey);
            document.addEventListener('keydown', keyDown);
            own.onKeyDown = keyDown;
            mouseControl.disableMouseControl();
            own.activateInput(activate, 10, 10);
            own.show();
        } else {
            document.removeEventListener('keydown', keyDown);
            own.onKeyDown = undefined;
            mouseControl.restoreMouseControl();
            own.hide();
        }
    }

    own['onApplicationShown'] = function () {
        log.debug('onApplicationShown.!!! in the Popup!');
        if (POPUP.TYPE.TEXT === popupType) {
            setTimeout(function() {
                log.debug('onApplicationShown, reply text popup!');
                setTimeout(function() {
                    navCoreProxy.enableIMELocation(350, 156);
                    log.debug('onApplicationShown, useTextArea!');
                    focusManager.useTextArea();
                }, 100);
            }, 100);
        }
    };

    function keyDown(e) { // KTKIDSCARE-126
        log.debug('keyDown: ' + (e && e.keyCode));
        switch (e.keyCode) {
        case VK_ESCAPE: 
            consumeEvent(e);
            hidePopup();
            log.debug('keyDown: hide and call consumeEvent');
            break;
        case VK_ENTER : 
            focusManager.doAction();
            consumeEvent(e);
            break;
        case VK_LEFT :
            focusManager.goLeft();
            consumeEvent(e);
            break;
        case VK_UP :
            focusManager.doAction();
            focusManager.goUp();
            consumeEvent(e);
            break;
        case VK_RIGHT :
            focusManager.goRight();
            consumeEvent(e);
            break;
        case VK_DOWN :
            focusManager.goDown();
            consumeEvent(e);
            break;
        case VK_BLUE :
            focusManager.goBlue();
            consumeEvent(e);
            break;
        case VK_BACK_SPACE : // 9
            focusManager.backSpace();
            consumeEvent(e);
            break;
        case VK_VOLUME_UP:
        case VK_VOLUME_DOWN:
        case VK_MUTE:
            break;
        case VK_BACK :
        case VK_RED :
        case VK_YELLOW :
            log.debug('keyDown: back or color(red, yellow), only call consumeEvent');
            consumeEvent(e);
            break;
        default : 
            log.debug('keyDown: default case, hide and don\'t call consumeEvent');
            hidePopup();
            break;
        }
    }

    function consumeEvent(e) {
        log.debug('consumeEvent, event=' + e);
        e.preventDefault();
        e.stopPropagation();
    }

    function showPopup(type, param, activate) {
        if (param) {
            log.debug('showPopup, set parameter=' + param);
            parameter = param;
        }

        if (POPUP.TYPE.NOTICE === type) {
            parameter[KEY.TITLE_PREFIX] = '';
            parameter[KEY.TITLE] = '공지사항';
        } else {
            if (POPUP.TYPE.TALK === type || POPUP.TYPE.PICTURE === type) {
                parameter[KEY.TITLE_PREFIX] = '보낸사람 : ';
            } else {
                parameter[KEY.TITLE_PREFIX] = '받는사람 : ';
            }
            parameter[KEY.TITLE] = getSender(param[KEY.PHONE]);
        }

        if (parameter[KEY.ERROR_TYPE]) {
            parameter[KEY.MESSAGE] = getErrorMessage(parameter[KEY.ERROR_TYPE]);
        }

        log.debug('showPopup, errorType=' + parameter[KEY.ERROR_TYPE]
                + ', message=' + parameter[KEY.MESSAGE]);

        popupType = type;
        focusManager = new FocusManager();

        switch (popupType) {
        case POPUP.TYPE.NOTICE:
            makeNoticeTag();
            break;
        case POPUP.TYPE.TALK:
            makeTalkTag();
            break;
        case POPUP.TYPE.PICTURE:
            loadPicture(parameter[KEY.IMAGE_URL]);
            break;
        case POPUP.TYPE.TEXT:
            makeReplyTextTag();
            break;
        case POPUP.TYPE.EMOTICON:
            loadEmoticonList();
            break;
        }

        setDisplayTime(param[KEY.DISPLAY_TIME]);

        if (own.visible) {
            log.debug('showPopup, already visible');
        } else {
            setVisible(true, activate);
        }
    }

    function getSender(phone) {
        var sender = '';
        if (isNaN(phone)) {
            sender = phone;
        } else if (phone.length === 10) {
            sender = phone.substring(0, 3) + CHAR.HYPHEN + phone.substring(3, 6)
                    + CHAR.HYPHEN + phone.substring(6);
        } else if (phone.length === 11) {
            sender = phone.substring(0, 3) + CHAR.HYPHEN + phone.substring(3, 7)
                    + CHAR.HYPHEN + phone.substring(7);
        } else {
            sender = phone;
        }
        return sender;
    }

    function setDisplayTime(displayTime) {
        if (displayTime) {
            var timeout = parseInt(displayTime);
            if (!isNaN(timeout) && timeout > 0) {
                log.debug('setDisplayTime, displayTime=' + displayTime);
                setTimeout(function() {
                    hidePopup();
                }, timeout * 1000);
            }
        }
    }

    function showReplyPopup(param) {
        if (param) {
            var isTextPopup = POPUP.TYPE.TEXT === param[KEY.REPLY];
            log.debug('showReplyPopup, param=' + param + ', isTextPopup=' + isTextPopup);
    
            if (isTextPopup) {
                setVisible(false);
            }
            showPopup(param[KEY.REPLY], param, isTextPopup);
        } else {
            log.error('showReplyPopup, invalid param=' + param);
        }
    }

    function replyMessage(param) {
        var serviceCode = param[KEY.SERVICE_CODE],
            phone = param[KEY.PHONE],
            uuid = param[KEY.UUID],
            said = param[KEY.SAID],
            message = param[KEY.REPLY_MESSAGE];

        log.debug('replyMessage, serviceCode=' + serviceCode + ', phone='
                + phone + ', uuid=' + uuid + ', message=' + message);
        hidePopup();
        kidsMethod.notifyExecuteResult({
            'method' : KEY.REQUEST,
            'CMD' : 'ETC0106',
            'SVC_CD' : serviceCode,
            'SAID' : said,
            'HP_NO' : phone,
            'HP_UUID' : uuid,
            'RESP_MSG' : message
        });
    }

    function hidePopup() {
        log.debug('hidePopup');
        clearLayout();
        if (POPUP.TYPE.TEXT === popupType) {
            navCoreProxy.disableIMELocation();
        }
        popupType = -1;
        setVisible(false);
    }

    function makeNoticeTag() {
        log.debug('makeNoticeTag');
        clearLayout();

        var popupDiv, bgDiv, messageDiv, buttonDiv,
            contentSize = 24, // margin between Title and Message
            marginMessageNButton = 20,
            marginButtonNShotcut = 3,
            marginButtonBottom = 6,
            bgSideHeight = 0,
            messageHeight = 0,
            buttonWidth = 0,
            shotcutHeight = 0,
            lineHeight = 0, maxLine = 0,
            popupHeight = 0,
            bottonAttrValue,
            styleName = POPUP.STYLE.NOTICE;

        $.makeDivAppendChild(layoutDiv, {
            'id': 'dim'
        });

        popupDiv = $.makeDivAppendChild(layoutDiv, {
            'id': 'popup',
            'class': 'popup_' + styleName
        });

        bgDiv = $.makeDivAppendChild(popupDiv, {
            'id': 'background',
        });

        bgSideHeight = addBackground(bgDiv, styleName, POPUP.BG_EXPAND.HEIGHT);
        addTitle(popupDiv, POPUP.TITLE_ICON.INFO);
        log.debug('background height=' + bgSideHeight);

        $.makeDivAppendChild(popupDiv, {
            'id': 'title_stroke',
            'class': 'title_stroke_' + styleName
        });

        messageDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'message_box',
            'class' : 'message_box_' + styleName
        }, $.replaceToBR(parameter[KEY.MESSAGE]));

        messageHeight = messageDiv.offsetHeight;
        contentSize += messageHeight;
        log.debug('message offsetHeight, messageHeight=' + messageHeight
                + ', contentSize=' + contentSize);
        contentSize += marginMessageNButton;

        buttonDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'button_box',
            'class' : 'button_box_' + styleName
        });

        buttonWidth = addButton(styleName, buttonDiv);

        contentSize += $.height(buttonDiv);
        log.debug('contentSize=' + contentSize + ', buttonDiv.height='
                + $.height(buttonDiv));

        bottonAttrValue = 'left:' + ($.width(popupDiv) - buttonWidth) / 2
                        + CHAR.PIXEL + '; width:' + buttonWidth + CHAR.PIXEL;

        if (parameter[KEY.SHOTCUT_NAME]) {
            shotcutHeight = getShortcutHeightAfterMake(popupDiv, styleName, marginButtonNShotcut);
            log.debug('shotcutDiv, height=' + shotcutHeight);

            contentSize += shotcutHeight;
            bottonAttrValue += '; bottom: ' + ($.bottom(buttonDiv) + shotcutHeight) + CHAR.PIXEL;
        }

        $.setAttribute(buttonDiv, {'style' : bottonAttrValue});
        contentSize += marginButtonBottom;

        if (VALUE.SCREEN_HEIGHT < bgSideHeight + contentSize) {
            contentSize -= messageHeight;
            lineHeight = $.lineHeight(messageDiv);
            maxLine = parseInt((VALUE.SCREEN_HEIGHT - bgSideHeight - contentSize) / lineHeight, 10);
            messageHeight = lineHeight * maxLine;
            contentSize += messageHeight;

            log.debug('message.height=' + messageHeight + ', height='
                    + $.height(messageDiv) + ', height=' + lineHeight);
            $.setAttribute(messageDiv, {
                'style' : 'height: ' + messageHeight + CHAR.PIXEL + '; max-height:'
                        + messageHeight + CHAR.PIXEL + '; -webkit-line-clamp: ' + maxLine
            });
        }

        popupHeight = bgSideHeight + contentSize;
        $.setAttribute(bgDiv.childNodes[1], {'style' : 'height:' + contentSize + CHAR.PIXEL});
        log.debug('popup.height=' + popupHeight + ', popup.top='
                + (VALUE.SCREEN_HEIGHT / 2 - popupHeight / 2));

        $.setAttribute(popupDiv, {
            'style': 'top: ' + (VALUE.SCREEN_HEIGHT / 2 - popupHeight / 2) + CHAR.PIXEL
        });
    }

    function addButton(styleName, buttonDiv) {
        var buttonWidth = 0,
            param = {},
            reply = parameter[KEY.REPLY];

        if (reply === 'text' || reply === 'emoticon') {
            buttonWidth = addReplyButton(styleName, buttonDiv);
            focusManager.setCurrentFocus(focusManager.FOCUS_NAME_ACTION);

            if (parameter[KEY.UUID]) {
                param[KEY.REPLY] = (reply === 'text' ? POPUP.TYPE.TEXT : POPUP.TYPE.EMOTICON);
                param[KEY.UUID] = parameter[KEY.UUID];
                param[KEY.PHONE] = parameter[KEY.PHONE];
                param[KEY.SAID] = parameter[KEY.SAID];
                param[KEY.SERVICE_CODE] = parameter[KEY.SERVICE_CODE];

                focusManager.setParam(focusManager.FOCUS_NAME_ACTION, param);
            }
        } else {
            buttonWidth = addConfirmButton(styleName, buttonDiv);
            focusManager.setCurrentFocus(focusManager.FOCUS_NAME_CLOSE);
        }
        return buttonWidth;
    }

    function addConfirmButton(styleName, buttonDiv) {
        return makeButtonDiv(styleName, [makeCloseFocus('확인')], buttonDiv);
    }

    function addReplyButton(styleName, buttonDiv) {
        return makeButtonDiv(styleName, [makeCloseFocus('닫기'),
                                         {'id': focusManager.FOCUS_NAME_ACTION,
                                        'name': '답장',
                                        'action': showReplyPopup}], buttonDiv);
    }

    function addSendButton(styleName, buttonDiv) {
        return makeButtonDiv(styleName, [makeCloseFocus('닫기'),
                                        {'id' : focusManager.FOCUS_NAME_ACTION,
                                        'name' : '보내기',
                                        'action' : replyMessage}], buttonDiv);
    }

    function makeCloseFocus(name) {
        return {
            'id': focusManager.FOCUS_NAME_CLOSE,
            'name': name,
            'action': hidePopup
        };
    }

    function makeButtonDiv(styleName, buttonArray, buttonDiv) {
        var i = 0,
            buttonCount = buttonArray.length,
            buttonObj,
            aButtonDiv,
            buttonType = buttonCount === 1 ? 'long_' : '',
            width = 0;

        for (i = 0; i < buttonCount; i++) {
            buttonObj = buttonArray[i];
            log.debug('[' + i + '] id=' + buttonObj['id'] + ', name='
                    + buttonObj['name'] + ', action=' + buttonObj['action']);

            aButtonDiv = $.makeDivAppendChild(buttonDiv, {
                'id': 'button_' + (i === 0 ? 'left' : 'right'),
                'class' : 'button_' + buttonType + styleName
                        + (styleName === POPUP.STYLE.REPLY && buttonObj['id'] === focusManager.FOCUS_NAME_ACTION
                            ? '_dimmedunfocus' : '_unfocus')
            });

            $.makeDivAppendChild(aButtonDiv, {
                'id': 'button_text'
            }, buttonObj['name']);

            focusManager.add(buttonObj['id'], aButtonDiv, buttonObj['action']);
            width += $.width(aButtonDiv);
        }
        log.debug('makeButtonDiv, width=' + width);
        if (buttonCount === 2) {
            width += 10; // margin between buttons
        }

        return width;
    }

    function makeTalkTag() {
        log.debug('makeTalkTag');
        clearLayout();

        var popupDiv, bgDiv, messageDiv, buttonDiv,
            marginMessageNButton = 15,
            marginButtonNShotcut = 3,
            overlapBgArea = 61, // 42=bg top area, 19=bg bottom area
            bgSideHeight = 0,
            messageHeight = 0,
            contentSize = 0,
            buttonWidth = 0,
            shotcutHeight = 0,
            bottonAttrValue,
            totalHeight = 0, validHeight = 0,
            lineHeight = 0, maxLine = 0,
            styleName = POPUP.STYLE.TALK;

        popupDiv = $.makeDivAppendChild(layoutDiv, {
            'id': 'popup',
            'class': 'popup_' + styleName
        });

        bgDiv = $.makeDivAppendChild(popupDiv, {
            'id': 'background'
        });

        bgSideHeight = addBackground(bgDiv, styleName, POPUP.BG_EXPAND.HEIGHT);
        addTitle(popupDiv, POPUP.TITLE_ICON.MAIL);
        log.debug('background height=' + bgSideHeight);

        $.makeDivAppendChild(popupDiv, {
            'id': 'title_stroke',
            'class': 'title_stroke_' + styleName
        });

        log.debug('message=' + parameter[KEY.MESSAGE]);

        messageDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'message_box',
            'class' : 'message_box_' + styleName
        }, $.replaceToBR(parameter[KEY.MESSAGE]));

        messageHeight = messageDiv.offsetHeight;
        log.debug('message offsetHeight, messageHeight=' + messageHeight
                + ', contentSize=' + contentSize);
        contentSize += marginMessageNButton;

        buttonDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'button_box',
            'class' : 'button_box_' + styleName
        });

        buttonWidth = addButton(styleName, buttonDiv);

        contentSize += $.height(buttonDiv);
        log.debug('contentSize=' + contentSize + ', buttonDiv.height='
                + $.height(buttonDiv));

        bottonAttrValue = 'left:' + ($.width(popupDiv) - buttonWidth) / 2
                        + CHAR.PIXEL + '; width:' + buttonWidth + CHAR.PIXEL;

        if (parameter[KEY.SHOTCUT_NAME]) {
            shotcutHeight = getShortcutHeightAfterMake(popupDiv, styleName, marginButtonNShotcut);
            log.debug('shotcutDiv, height=' + shotcutHeight);

            contentSize += shotcutHeight;
            bottonAttrValue += '; bottom: ' + ($.bottom(buttonDiv) + shotcutHeight) + CHAR.PIXEL;
        }

        $.setAttribute(buttonDiv, {'style' : bottonAttrValue});
        totalHeight = bgSideHeight - overlapBgArea + contentSize + messageHeight;
        validHeight = VALUE.SCREEN_HEIGHT - $.bottom(popupDiv);
        log.debug('total height=' + totalHeight);

        if (validHeight < totalHeight) {
            lineHeight = $.lineHeight(messageDiv);
            maxLine = parseInt((validHeight - bgSideHeight + overlapBgArea - contentSize) / lineHeight, 10);
            messageHeight = lineHeight * maxLine;
            log.debug('message.height=' + messageHeight + ', height='
                    + $.height(messageDiv) + ', height=' + lineHeight + ', count=' + maxLine);
            $.setAttribute(messageDiv, {
                'style' : 'height: ' + messageHeight + CHAR.PIXEL + '; max-height:'
                    + messageHeight + CHAR.PIXEL + '; -webkit-line-clamp: ' + maxLine
            });
        }
        contentSize += messageHeight;

        log.debug('bgMiddle.height=' + (contentSize - overlapBgArea)); 
        $.setAttribute(bgDiv.childNodes[1], {'style' : 'height:' + (contentSize - overlapBgArea) + CHAR.PIXEL});
    }

    function loadPicture(url) {
        log.debug('loadPicture, url=' + url);

        var pictureImg = $.makeElement('img', {
            'id': 'picture'
        });
        pictureImg.src = url;
        pictureImg.onload = function() {
            log.debug('onload, img width=' + pictureImg.width + ', height='
                    + pictureImg.height);

            if (pictureImg.width && pictureImg.height) {
                var picInfo = getImageSize(pictureImg.width, pictureImg.height);
                log.debug('img, getImageSize=' + picInfo['width'] + ', height='
                        + picInfo['height']);
                callbackResult(ERROR_CODE.SUCCESS_000);
                makePictureTag(POPUP.BG_EXPAND.HEIGHT, pictureImg, picInfo['width'], picInfo['height']);
            } else {
                log.error('onload, invalid img size');
                showImageError(ERROR_CODE.IMAGE_OTHER_ERROR);
            }
        };
        pictureImg.onerror = function() {
            log.debug('onerror');
            showImageError(ERROR_CODE.IMAGE_OTHER_ERROR);
        };
    }

    function callbackResult(code) {
        log.debug('callbackResult, code=' + code);
        kidsMethod.notifyExecuteResult({
            'method' : 'showPopup',
            'resultCode' : code + ''
        });
    }

    function showImageError(type) {
        var param = {};

        param[KEY.MESSAGE] = getErrorMessage(type);
        param[KEY.PHONE] = parameter[KEY.PHONE];

        callbackResult(type);

        showPopup(POPUP.TYPE.TALK, param);
    }

    function getErrorMessage(type) {
        var message = '';
        switch (parseInt(type, 10)) {
        case ERROR_CODE.IMAGE_TOO_MUCH_SIZE :
            message = '수신된 사진의 사이즈가 커서 표시할 수 없습니다. 전송한 사진의 크기를 줄이신 후 다시 시도해 주세요.';
            break;
        case ERROR_CODE.IMAGE_UNSUPPORT_FORMAT :
            message = '수신된 사진의 포맷을 지원하지 않습니다. 지원되는 포맷은 JPG와 PNG입니다. 다시 확인해 주세요.';
            break;
        case ERROR_CODE.IMAGE_UNSUPPORT_MODEL :
            message = '사진 보기 서비스가 지원하지 않는 단말입니다.';
            break;
        case ERROR_CODE.IMAGE_OTHER_ERROR :
            message = '오류가 발생해 수신된 이미지를 표시할 수 없습니다.';
            break;
        }
        return message;
    }

    function makePictureTag(expandType, pictureImg, pictureWidth, pictureHeight) {
        if (POPUP.BG_EXPAND.HEIGHT === expandType) {
            makePictureTagExpandH(pictureImg, pictureWidth, pictureHeight);
        } else {
            makePictureTagExpandW(pictureImg, pictureWidth, pictureHeight);
        }
    }

    function makePictureTagExpandW(pictureImg, pictureWidth, pictureHeight) {
        log.debug('makePictureTag(expandable width)');
        clearLayout();

        var popupDiv, bgDiv, strokeDiv, buttonDiv,
            popupWidth = 0,
            contentWidth = 0,
            marginPictureWidth = 0,
            buttonWidth = 0,
            shotcutHeight = 0,
            bottonAttrValue,
            styleName = BG_TYPE_PICTURE + CHAR.UNDERSCORE + POPUP.BG_EXPAND.WIDTH;

        popupDiv = $.makeDivAppendChild(layoutDiv, {
            'id' : 'popup',
            'class' : 'popup_' + styleName
        });

        bgDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'background'
        });

        if (pictureWidth < POPUP.PICTURE_SIZE.WH423) {
            contentWidth = POPUP.PICTURE_SIZE.WH423 - pictureWidth;
            marginPictureWidth = parseInt(contentWidth / 2, 10);
        }
        contentWidth += pictureWidth;

        popupWidth = contentWidth + addBackground(bgDiv, styleName, POPUP.BG_EXPAND.WIDTH);
        $.setAttribute(popupDiv, {
            'style' : 'width: ' + popupWidth + CHAR.PIXEL
        });

        addTitle(popupDiv, POPUP.TITLE_ICON.MAIL);

        strokeDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'title_stroke',
            'class' : 'title_stroke_' + styleName
        });
        $.setAttribute(strokeDiv, {
            'style' : 'left: ' + (80 + (contentWidth - $.width(strokeDiv)) / 2) + CHAR.PIXEL
        });

        $.setAttribute(pictureImg, {
            'class' : styleName,
            'width' : pictureWidth + CHAR.PIXEL,
            'height' : pictureHeight + CHAR.PIXEL
        });
        $.appendChild(popupDiv, pictureImg);

        if (marginPictureWidth) {
            $.setAttribute(pictureImg, {
                'style' : 'left: ' + (80 + marginPictureWidth) + CHAR.PIXEL
                        + '; top: ' + 96 + CHAR.PIXEL
            }) ;
        }

        buttonDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'button_box',
            'class' : 'button_box_' + styleName
        });

        buttonWidth = addButton(styleName, buttonDiv);
        bottonAttrValue = 'left: ' + (popupWidth - buttonWidth) / 2 + CHAR.PIXEL
                        + '; width: ' + buttonWidth + CHAR.PIXEL;

        if (parameter[KEY.SHOTCUT_NAME]) {
            shotcutHeight = getShortcutHeightAfterMake(popupDiv, styleName, 3);  // 3 is margin Button and Shotcut
            log.debug('shotcutDiv, height=' + shotcutHeight);

            bottonAttrValue += '; bottom: ' + ($.bottom(buttonDiv) + shotcutHeight / 2) + CHAR.PIXEL;
        }

        $.setAttribute(buttonDiv, {'style' : bottonAttrValue});

        log.debug('bgMiddle.width=' + contentWidth); 
        $.setAttribute(bgDiv.childNodes[1], {'style' : 'width: ' + contentWidth + CHAR.PIXEL});
    }

    function makePictureTagExpandH(pictureImg, pictureWidth, pictureHeight) {
        log.debug('makePictureTag(expandable height)');
        clearLayout();

        var popupDiv, bgDiv, strokeDiv, buttonDiv,
            contentCenter = 305,
            popupHeight = 0,
            marginPictureWidth = 0,
            buttonWidth = 0,
            shotcutHeight = 0,
            bottonAttrValue,
            styleName = POPUP.STYLE.PICTURE + CHAR.UNDERSCORE + POPUP.BG_EXPAND.HEIGHT;

        popupDiv = $.makeDivAppendChild(layoutDiv, {
            'id' : 'popup',
            'class' : 'popup_' + styleName
        });

        bgDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'background'
        });

        if (pictureWidth < POPUP.PICTURE_SIZE.WH423) {
            marginPictureWidth = parseInt((POPUP.PICTURE_SIZE.WH423 - pictureWidth) / 2, 10);
        }

        popupHeight = addBackground(bgDiv, styleName, POPUP.BG_EXPAND.HEIGHT) + pictureHeight;
 
        addTitle(popupDiv, POPUP.TITLE_ICON.MAIL);

        strokeDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'title_stroke',
            'class' : 'title_stroke_' + styleName
        });
        $.setAttribute(strokeDiv, {
            'style' : 'left: ' + (contentCenter - $.width(strokeDiv) / 2) + CHAR.PIXEL
        });

        $.setAttribute(pictureImg, {
            'class' : styleName,
            'width' : pictureWidth + CHAR.PIXEL,
            'height' : pictureHeight + CHAR.PIXEL
        });
        $.appendChild(popupDiv, pictureImg);

        if (marginPictureWidth) {
            $.setAttribute(pictureImg, {
                'style' : 'left: ' + (94 + marginPictureWidth) + CHAR.PIXEL
                        + '; top: ' + 96 + CHAR.PIXEL
            }) ;
        }

        buttonDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'button_box',
            'class' : 'button_box_' + styleName
        });

        buttonWidth = addButton(styleName, buttonDiv);
        bottonAttrValue = 'left: ' + (contentCenter - buttonWidth / 2) + CHAR.PIXEL
                        + '; width: ' + buttonWidth + CHAR.PIXEL;

        if (parameter[KEY.SHOTCUT_NAME]) {
            shotcutHeight = getShortcutHeightAfterMake(popupDiv, styleName, 3);  // 3 is margin Button and Shotcut
            log.debug('shotcutDiv, height=' + shotcutHeight);

            bottonAttrValue += '; bottom: ' + ($.bottom(buttonDiv) + shotcutHeight) + CHAR.PIXEL;
        }

        $.setAttribute(buttonDiv, {'style' : bottonAttrValue});

        $.setAttribute(popupDiv, {
            'style' : 'height: ' + (popupHeight + shotcutHeight) + CHAR.PIXEL
        });

        log.debug('bgMiddle.height=' + pictureHeight); 
        $.setAttribute(bgDiv.childNodes[1], {'style' : 'height: ' + (pictureHeight + shotcutHeight) + CHAR.PIXEL});
    }

    function makeReplyTextTag() {
        log.debug('makeReplyTextTag');
        clearLayout();

        var popupDiv, bgDiv, buttonDiv,
            replyInput,
            overlapBgArea = 61, // 42=bg top area, 19=bg bottom area
            bgSideHeight = 0,
            contentSize = 0,
            buttonWidth = 0,
            styleName = POPUP.STYLE.REPLY;

        popupDiv = $.makeDivAppendChild(layoutDiv, {
            'id': 'popup',
            'class': 'popup_' + styleName
        });

        bgDiv = $.makeDivAppendChild(popupDiv, {
            'id': 'background'
        });

        bgSideHeight = addBackground(bgDiv, POPUP.STYLE.TALK, POPUP.BG_EXPAND.HEIGHT);
        addTitle(popupDiv, POPUP.TITLE_ICON.MAIL);
        log.debug('background height=' + bgSideHeight);

        $.makeDivAppendChild(popupDiv, {
            'id': 'title_stroke',
            'class': 'title_stroke_' + styleName
        });

        replyInput = $.makeElement('textarea', {
            'id': 'reply_text',
            'type' : 'text',
            'rows' : 3,
            'cols' : POPUP.COLUMN_SIZE,
            'maxLength' : 40,
            'x-altibrowser-imemode' : 'KOR'
        }, popupDiv);

        contentSize += $.height(replyInput) + 15; // 15 is margin Message and Button
        focusManager.add(focusManager.FOCUS_NAME_CONTENT, replyInput, replyMessage);

        buttonDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'button_box',
            'class' : 'button_box_' + styleName
        });

        buttonWidth = addSendButton(styleName, buttonDiv);

        contentSize += $.height(buttonDiv);
        log.debug('contentSize=' + contentSize + ', buttonDiv.height='
                + $.height(buttonDiv));

        $.setAttribute(buttonDiv, {
            'style' : 'left:' + ($.width(popupDiv) - buttonWidth) / 2 + CHAR.PIXEL
                    + ';  width:' + buttonWidth + CHAR.PIXEL
        });

        log.debug('bgMiddle.height=' + (contentSize - overlapBgArea)); 
        $.setAttribute(bgDiv.childNodes[1], {'style' : 'height:' + (contentSize - overlapBgArea) + CHAR.PIXEL});
    }

    function makeReplyEmoticonTag() {
        log.debug('makeReplyEmoticonTag');
        clearLayout();

        var popupDiv, bgDiv, replyEmoticonDiv, buttonDiv,
            overlapBgArea = 80, // 54=bg top area, 26=bg bottom area
            bgSideHeight = 0,
            contentSize = 0,
            buttonWidth = 0,
            styleName = POPUP.STYLE.REPLY;

        popupDiv = $.makeDivAppendChild(layoutDiv, {
            'id': 'popup',
            'class': 'popup_' + styleName
        });

        bgDiv = $.makeDivAppendChild(popupDiv, {
            'id': 'background'
        });

        bgSideHeight = addBackground(bgDiv, POPUP.STYLE.TALK, POPUP.BG_EXPAND.HEIGHT);
        addTitle(popupDiv, POPUP.TITLE_ICON.MAIL);
        log.debug('background height=' + bgSideHeight);

        $.makeDivAppendChild(popupDiv, {
            'id': 'title_stroke',
            'class': 'title_stroke_' + styleName
        });

        replyEmoticonDiv = $.makeDivAppendChild(popupDiv, {
            'id': 'reply_emoticon'
        });

        if (focusManager.manyPage()) {
            $.makeDivAppendChild(replyEmoticonDiv, {
                'id': 'arrow_left'
            });
            $.makeDivAppendChild(replyEmoticonDiv, {
                'id': 'arrow_right'
            });
        }

        canvasElement = $.makeElement('canvas', {
            'id': 'emoticon_list',
        }, replyEmoticonDiv);
        canvasElement.width = 337;
        canvasElement.height = 136;

        contentSize += $.height(replyEmoticonDiv) + 12; // 12 is margin Content and Button
        focusManager.add(focusManager.FOCUS_NAME_CONTENT, canvasElement, replyMessage);

        buttonDiv = $.makeDivAppendChild(popupDiv, {
            'id' : 'button_box',
            'class' : 'button_box_' + styleName
        });

        buttonWidth = addSendButton(styleName, buttonDiv);
        focusManager.setCurrentFocus(focusManager.FOCUS_NAME_CONTENT);
        focusManager.drawEmoticonList();

        contentSize += $.height(buttonDiv);
        log.debug('contentSize=' + contentSize + ', buttonDiv.height='
                + $.height(buttonDiv));

        $.setAttribute(buttonDiv, {
            'style' : 'left:' + ($.width(popupDiv) - buttonWidth) / 2 + CHAR.PIXEL
                    + '; width:' + buttonWidth + CHAR.PIXEL
        });

        log.debug('bgMiddle.height=' + (contentSize - overlapBgArea)); 
        $.setAttribute(bgDiv.childNodes[1], {'style' : 'height:' + (contentSize - overlapBgArea) + CHAR.PIXEL});
    }

    /*
     * 요구사항(1280x720)
     * > 1:1 비율 사진은 가로 423px, 세로 423px 크기로 센터 영역에 표시
     * > 가로 방향 사진 : 가로 423px로 고정, 세로를 비율에 맞게 변경
     * > 세로 방향 사진 : 세로 423px로 고정, 가로를 비율에 맞게 변경
     */
    function getImageSize(width, height) {
        var picWidth = 0,
            picHeight = 0;

        if (width <= 0 || height <= 0) {
            
        } else if (width == height) { // 1:1 비율(423, 423)
            log.debug('getImageSize, 1:1 rate(' + width + ':' + height + ')');
            picWidth = POPUP.PICTURE_SIZE.WH423;
            picHeight = POPUP.PICTURE_SIZE.WH423;
        } else { // 기타 비율(가로:세로 비율 유지, 가로 최대 423, 세로 최대 423)
            log.debug('getImageSize, other(' + width + ':' + height + ')');
            var rate = 0,
                type = null;

            if (width > height) { // 가로가 더 긴 이미지
                type = POPUP.PICTURE_SIZE.WH423 > width ? '가로=확대필요' : '가로=축소필요';
                rate = POPUP.PICTURE_SIZE.WH423 / width;
                log.debug('getImageSize, other(long width) ' + type + ', rate='
                        + rate);

                var resizeHeight = POPUP.PICTURE_SIZE.MAX_HEIGHT < (height * rate);
                log.debug('getImageSize, other(long width) ' + type + ', 세로='
                        + (resizeHeight ? '초과' : '적당'));

                if (resizeHeight) {
                    rate = POPUP.PICTURE_SIZE.MAX_HEIGHT / height;
                    log.debug('getImageSize, other(long width) ' + type
                            + ', 가로=비율 재조정(원본 사이즈 기준)');
                }
                log.debug('getImageSize, other(long width), rate=<' + rate + '>');
            } else { // 세로가 더 긴 이미지
                type = POPUP.PICTURE_SIZE.WH423 > height ? '세로=확대필요' : '세로=축소필요';
                rate = POPUP.PICTURE_SIZE.WH423 / height;

                log.debug('getImageSize, other(long height) ' + type + ', rate='
                        + rate);
                var resizeWidth = POPUP.PICTURE_SIZE.WH423 < width * rate;
                log.debug('getImageSize, other(long height) ' + type + ', 가로='
                        + (resizeWidth ? '초과' : '적당'));
                if (resizeWidth) {
                    var baseWidth = width < POPUP.PICTURE_SIZE.WH423; 
                    rate = baseWidth ?  width / POPUP.PICTURE_SIZE.WH423 : POPUP.PICTURE_SIZE.WH423 / width;
                    log.debug('getImageSize, other(long height) ' + type
                            + ', 가로=비율 재조정(' + (baseWidth ? '지정' : '원본' ) + ' 사이즈 기준)');
                }

                log.debug('getImageSize, other(long height) rate=<' + rate + '>');
            }
            picWidth = parseInt(width * rate, 10);
            picHeight = parseInt(height * rate, 10);
        }
        log.debug('getImageSize, final image size=' + picWidth + ':' + picHeight);

        return {'width' : picWidth, 'height' : picHeight};
    }

    function clearLayout() {
        focusManager.clearTextArea();
        if (canvasElement) {
            canvasElement = null;
        }
        $.clearElement(layoutDiv);
    }

    function IconTag() {
        this.url;
        this.desc;
    }

    function responseEmoticonInfo(isSuccess, responseXML) {
        log.debug('responseEmoticonInfo, isSuccess=' + isSuccess
                + ', responseXML=' + responseXML);
        var rootChild, 
            iconList;

        if (isSuccess && responseXML) {
            rootChild = responseXML.firstChild,
            iconList = rootChild.getElementsByTagName('icon');
            log.debug('iconList(' + iconList.length + ')=' + iconList);

            if (iconList && iconList.length > 0) {
                focusManager.setEmoticonUrl(parseIconTag(iconList));
                log.debug('responseEmoticonInfo, setEmoticonUrl');
            }
            makeReplyEmoticonTag();
        } else {
            log.error('responseEmoticonInfo, fail emoticon url!');
        }
    }

    function parseIconTag(iconList) {
        var i, j,
            node, nodeName,
            count = iconList.length,
            iconTag,
            children,
            iconArray = [];

        for (i = 0; i < count; i++) {
            children = iconList[i].childNodes;
            iconTag = new IconTag();

            for (j = 0; j < children.length; j++) {
                node = children[j];
                nodeName = node.nodeName;

                if (nodeName === 'url') {
                    iconTag.url = node.textContent.trim();
                    log.info('[' + j + '] url=' + iconTag.url);
                } else if (nodeName === 'desc') {
                    iconTag.desc = node.textContent.trim();
                    log.info('[' + j + '] desc=' + iconTag.desc);
                }
            }
            iconArray[i] = iconTag;
            j = 0;
        }

        log.debug('parseIconTag, iconList.length='+ count
                + ', iconArray.lenght=' + iconArray.length);

        /*
        for (i = 0; i < iconArray.length; i++) {
            log.debug('[' + i + '] desc=' + iconArray[i].desc + ', url=' + iconArray[i].url);
        }
        */
        return iconArray;
    }

    function loadEmoticonList() {
        log.debug('loadEmoticonList, ajax=' + ajax);
        ajax.setCallbackType(ajax.CALLBACK_TYPE_XML);
        ajax.setTimeout(10000, responseEmoticonInfo);
        ajax.setCallbackListener(responseEmoticonInfo, responseEmoticonInfo);
        ajax.sendPostMethod(config.getKakaoURL() + '/WEB/image_icon.xml');
    }

    function addBackground(bgDiv, styleName, expandType) {
        var size = 0,
            firstBgDiv, lastBgDiv;

        if (POPUP.BG_EXPAND.WIDTH === expandType) {
            firstBgDiv = $.makeDivAppendChild(bgDiv, {
                'id': 'bg_left',
                'class': 'bg_left_' + styleName
            });
            $.makeDivAppendChild(bgDiv, {
                'id': 'bg_center',
                'class': 'bg_center_' + styleName
            });
            lastBgDiv = $.makeDivAppendChild(bgDiv, {
                'id': 'bg_right',
                'class': 'bg_right_' + styleName
            });

            size = $.width(firstBgDiv) + $.width(lastBgDiv);
        } else {
            firstBgDiv = $.makeDivAppendChild(bgDiv, {
                'id': 'bg_top',
                'class': 'bg_top_' + styleName
            });
            $.makeDivAppendChild(bgDiv, {
                'id': 'bg_middle',
                'class': 'bg_middle_' + styleName,
            });
            lastBgDiv = $.makeDivAppendChild(bgDiv, {
                'id': 'bg_bottom',
                'class': 'bg_bottom_' + styleName
            });

            size = $.height(firstBgDiv) + $.height(lastBgDiv);
        }

        return size;
    }

    function addTitle(popupDiv, iconType) {
        var title = parameter[KEY.TITLE_PREFIX] + parameter[KEY.TITLE],
            titleLeft, titleWidth, titleWidthByCSS,
            textWidth,
            titleDiv = $.makeDivAppendChild(popupDiv, {
                'id' : 'title',
                'class' : 'title_icon_' + iconType
            }, title);

        titleWidthByCSS = $.width(titleDiv); // 349 is title width in the css
        textWidthDiv.innerHTML = title;
        textWidth = textWidthDiv.offsetWidth;

        if (textWidth > titleWidthByCSS) {
            titleLeft = $.left(titleDiv);
            titleWidth = titleWidthByCSS;
        } else {
            titleWidth = $.textIndent(titleDiv) + textWidth;
            titleLeft = $.width(popupDiv) / 2 - titleWidth / 2;
        }
        log.debug('addTitle, textWidth=' + textWidth + ', titleWidthByCSS='
                + titleWidthByCSS + ', title.style.left=' + titleLeft
                + ', title.style.width=' + titleWidth);

        titleDiv.style.left = titleLeft + CHAR.PIXEL;
        titleDiv.style.width = titleWidth + CHAR.PIXEL;
    }

    function getShortcutHeightAfterMake(popupDiv, styleName, offset) {
        var shortcutDiv = makeShortcutDiv(popupDiv, styleName);

        addShortcutButton(shortcutDiv);

        return offset + $.height(shortcutDiv);
    }

    function makeShortcutDiv(popupDiv, styleName) {
        return $.makeDivAppendChild(popupDiv, {
            'id': 'shotcut',
            'class': 'shotcut_' + styleName
        }, parameter[KEY.SHOTCUT_NAME]);
    }

    function addShortcutButton(shortcutDiv) {
        var shortcutType = parseInt(parameter[KEY.SHOTCUT_TYPE], 10),
            param = {
                'type' : shortcutType,
                'url' : parameter[KEY.SHOTCUT_URL]
            };

        if (VALUE.SHORTCUT.CHILD_APP === shortcutType) {
            param.appID = parameter[KEY.SHORTCUT_APPID];
        }

        focusManager.add(focusManager.FOCUS_NAME_SHOTCUT, shortcutDiv, kidsMethod.goShortcut);
        focusManager.setParam(focusManager.FOCUS_NAME_SHOTCUT, param);
    }

    function FocusManager() {
        var FOCUS_NAME_CONTENT = 'content',
            FOCUS_NAME_SHOTCUT = 'shotcut',
            FOCUS_NAME_CLOSE = 'close',
            FOCUS_NAME_ACTION = 'action';

        var currentFocusName = undefined,
            lastButtonFocusName = FOCUS_NAME_CLOSE,
            movableButton = false,
            hasContent = false,
            position = {};

        var ROW_EMOTICON = 2,
            COLUMN_EMOTICON = 5,
            focusImage = null,
            context = null,
            countByPage = ROW_EMOTICON * COLUMN_EMOTICON,
            focusIndex = 0,
            emoticonArray,
            emoticonImgArray = [],
            inputIME = function() {
                log.debug('inputIME');
                var textArea = getElement(FOCUS_NAME_CONTENT),
                    actionButton,
                    currentClassName,
                    className,
                    valueCount;

                if (textArea) {
                    valueCount = textArea.value.length;
                    actionButton = getElement(FOCUS_NAME_ACTION);
                    currentClassName = $.getClass(actionButton);
                    log.debug('inputIME, valueCount=' + valueCount
                            + ', currentClassName=' + currentClassName);

                    if (currentClassName) {
                        if (valueCount === 0) {
                            className = currentClassName.substring(0, currentClassName.lastIndexOf('_') + 1);

                            $.setAttribute(actionButton, {
                                'class': className + 'dimmedunfocus'
                            });
                            setParam(FOCUS_NAME_ACTION, undefined);
                            checkMovableButton();
                            setLastButton(FOCUS_NAME_CLOSE);
                        } else if (valueCount === 1
                                && currentClassName.indexOf('dimmedunfocus') !== -1) {
                            className = currentClassName.substring(0, currentClassName.lastIndexOf('_') + 1);

                            $.setAttribute(actionButton, {
                                'class': className + 'unfocus'
                            });
                            setLastButton(FOCUS_NAME_ACTION);
                        }
                    }
                }
            };

        function setCurrentFocus(focusName) {
            log.debug('setCurrentFocus, focusName=' + focusName);
            setFocus(focusName);

            checkMovableButton();
            if (position[FOCUS_NAME_CONTENT]) {
                hasContent = true;
            }
        }

        function setEmoticonUrl(iconArray) {
            emoticonArray = iconArray;
            log.debug('setEmoticonUrl, size=' + getTotalCount());
        }

        function getTotalCount() {
            return emoticonArray.length;
        }

        function manyPage() {
            if (emoticonArray) {
                return getTotalCount() > countByPage;
            }
            return false;
        }

        function add(name, element, action) {
//            log.debug('add, name=' + name + ', action=' + action);
            position[name] = {'element' : element, 'action' : action};
        }

        function setParam(name, parameter) {
            log.debug('setParam, name=' + name + ', param=' + parameter);
            position[name]['param'] = parameter;
        }

        function goUp() {
            if (hasContentFocus()) {
                if (isEmoticonContent()) {
                    log.debug('goUp, row=' + getFocusRow());
                    if (getFocusRow() != 0) {
                        focusIndex -= COLUMN_EMOTICON;
                        drawEmoticonList();
                    }
                } else {
                    log.debug('goUp, textarea');
                    moveCursor(true, true);
                }
            } else if (hasContent && hasButtonFocus()) {
                setLastButton(currentFocusName);
                focusInContent();
                if (isEmoticonContent()) {
                    drawEmoticonList();
                } else {
                    moveCursorLast();
                }
            }
        }

        function goDown() {
            if (hasContentFocus()) {
                if (isEmoticonContent()) {
                    log.debug('goDown, row=' + getFocusRow());
                    if (getFocusRow() < ROW_EMOTICON - 1
                        && focusIndex + COLUMN_EMOTICON < getTotalCount()) {
                        focusIndex += COLUMN_EMOTICON;
                        drawEmoticonList();
                    } else {
                        focusOutEmoticon(lastButtonFocusName);
                    }
                } else {
                    log.debug('goDown, textarea');
                    moveCursor(true, false);
                }
            }
        }

        function goLeft() {
            log.debug('goLeft, currentFocus=' + currentFocusName
                    + ', hasButtonFocus=' + hasButtonFocus());
            if (hasButtonFocus()) {
                if (movableButton) {
                    moveFocusButton();
                }
            } else if (hasContentFocus()) {
                if (isEmoticonContent()) {
                    if (isFirstColumn()) {
                        var pageNum = getCurrentPage();

                        if (isItemFull()) {
                            var interval = COLUMN_EMOTICON + 1;
                            if (focusIndex - interval > 0) {
                                focusIndex -= interval;
                            } else {
                                focusIndex += (getTotalCount() - interval);
                            }
                        } else if (isFirstPage()) {
                            var nextIndex = getFirstIndexOfLastPage() + getFocusRow() * COLUMN_EMOTICON + (COLUMN_EMOTICON - 1);
                            log.debug('goLeft, lastPage index='
                                    + getFirstIndexOfLastPage() + ', row='
                                    + getFocusRow() * COLUMN_EMOTICON);
                            if (nextIndex >= getTotalCount()) {
                                focusIndex = getTotalCount() - 1;
                            } else {
                                focusIndex = nextIndex;
                            }
                            log.debug('goLeft, nextIndex=' + nextIndex
                                    + ', totalCount=' + getTotalCount()
                                    + ', focusIndex=' + focusIndex);
                        } else {
                            focusIndex -= (COLUMN_EMOTICON + 1);
                        }
                        if (pageNum !== getCurrentPage()) {
                            clearLoadImage();
                        }
                    } else {
                        focusIndex--;
                    }
                    drawEmoticonList();
                } else {
                    moveCursor(false, true);
                }
            }
        }

        function goRight() {
            log.debug('goRight, currentFocus=' + currentFocusName
                    + ', hasButtonFocus=' + hasButtonFocus());
            if (hasButtonFocus()) {
                if (movableButton) {
                    moveFocusButton();
                }
            } else if (hasContentFocus()) {
                if (isEmoticonContent()) {
                    if (isLastPage()) {
                        log.debug('goRight, it is lastPage, focusIndex='
                                + focusIndex);
                        var nextIndex = focusIndex + 1;
                        if (nextIndex % COLUMN_EMOTICON === 0
                            || nextIndex >= getTotalCount()) {
                            log.debug('goRight, it is lastPage, getRow(focusIndex)='
                                    + getFocusRow());
                            focusIndex = getFocusRow() * COLUMN_EMOTICON;
                            clearLoadImage();
                        } else {
                            focusIndex++;
                        }
                        log.debug('goRight, it is lastPage, final focusIndex='
                                + focusIndex);
                    } else {
                        log.debug('goRight, it isn\'t lastPage');
                        var interval = COLUMN_EMOTICON + 1;
                        if (isLastColumn() && focusIndex + interval < getTotalCount()) {
                            focusIndex += interval;
                            clearLoadImage();
                        } else {
                            focusIndex++;
                        }
                    }
                    drawEmoticonList();
                } else {
                    moveCursor(false, false);
                }
            }
        }

        function moveCursor(isUpDown, isUpLeft) {
            log.debug('moveCursor, isUpDown=' + isUpDown + ', isUpLeft='
                    + isUpLeft);

            var textArea = position[FOCUS_NAME_CONTENT]['element'],
                pos = 0,
                textSize = 0,
                selectionEnd = textArea.selectionEnd;
            log.debug('moveCursor, textArea.selectionStart='
                    + textArea.selectionStart + ', selectionEnd=' + textArea.selectionEnd);

            if (isUpDown) { // up or down
                if (isUpLeft) { // up
                    if (selectionEnd > POPUP.COLUMN_SIZE) {
                        pos = selectionEnd - POPUP.COLUMN_SIZE;
                        if (pos < 0) {
                            pos = 0;
                        }
                    }
                } else { // down
                    textSize = textArea.value.length;
                    log.debug('moveCursor, down, textSize=' + textSize
                            + ', first=' + (textSize > POPUP.COLUMN_SIZE)
                            + ', second=' + (selectionEnd <= POPUP.COLUMN_SIZE));
                    if (textSize > COLUMN_SIZE && selectionEnd <= POPUP.COLUMN_SIZE) {
                        pos = selectionEnd + POPUP.COLUMN_SIZE;
                        if (pos > textSize) {
                            pos = textSize;
                        }
                    } else {
                        focusOutInput(lastButtonFocusName);
                    }
                }
            } else { // left or right
                if (isUpLeft) { // left
                    pos = textArea.selectionEnd > 0 ? textArea.selectionEnd - 1 : 0;
                } else { // right
                    textSize = textArea.value.length;
                    log.debug('moveCursor, right, textSize=' + textSize);
                    pos = textArea.selectionEnd < textArea.value.length ? textArea.selectionEnd + 1 : textArea.value.length;
                }
            }

            if (textArea.setSelectionRange) {
                log.debug('setSelectionRange, pos=' + pos);
                textArea.focus();
                textArea.setSelectionRange(pos, pos);
            }
        }

        function moveCursorLast() {
            log.debug('moveCursorLast');

            var textArea = position[FOCUS_NAME_CONTENT]['element'],
                 pos = textArea.value.length;

            log.debug('moveCursorLast, pos=' + pos);
            textArea.focus();
            textArea.setSelectionRange(pos, pos);
        }

        function backSpace() {
            var textArea,
                value;

            if (hasContentFocus()) {
                textArea = getElement(FOCUS_NAME_CONTENT);
                if (isTextAreaTag(textArea) && textArea.value !== '') {
                    value = textArea.value;
                    log.debug('backSpace, value=' + value);
                    textArea.value = value.substring(0, value.length - 1);
                    inputIME();
                }
            }
        }

        function hasParam() {
            var param = getParam();
//            log.debug('hasParam, action[\'param\']=' + param);
            return param !== undefined;
        }

        function getParam() {
            var action = position[FOCUS_NAME_ACTION];
            log.debug('getParam, action[\'param\']=' + action['param']);
            return action['param'];
        }

        function checkMovableButton() {
            var className;

            if (position[FOCUS_NAME_CLOSE] && position[FOCUS_NAME_ACTION]) {

                className = $.getClass(getElement(FOCUS_NAME_ACTION));
                if (isReplyStyle(className)) {
                    movableButton = hasParam();
                } else {
                    movableButton = true;
                }
                log.debug('checkMovableButton, movableButton=' + movableButton);
            }
        }

        function isReplyStyle(className) {
            return className && className.indexOf(POPUP.STYLE.REPLY) !== -1;
        }

        function getElement(name) {
            try {
                if (position[name]) {
                    return position[name]['element'];
                } else {
                    log.error('getElement(' + name + '), doesn\'t exist at the position')
                }
            } catch (ex) {
                log.error('getElement(' + name + '), ex=' + ex);
            }
            return null;
        }

        function isTextAreaTag(element) {
            return element && element.tagName === 'TEXTAREA';
        }

        function isCanvasTag(element) {
            return element && element.tagName === 'CANVAS';
        }

        function isEmoticonContent() {
            return isCanvasTag(getElement(FOCUS_NAME_CONTENT));
        }

        function isFirstColumn() {
            return getFocusColumn() === 0;
        }

        function isLastColumn() {
            return getFocusColumn() === COLUMN_EMOTICON - 1;
        }

        function getCurrentPage() {
            var focusValue = focusIndex + 1,
                focusPage = parseInt(focusValue / countByPage, 10)
                            + (focusValue % countByPage > 0 ? 1 : 0);
            log.debug('getCurrentPage, page=' + focusPage);
            return focusPage;
        }

        function isFirstPage() {
            return getCurrentPage() === 1;
        }

        function getFirstIndexOfLastPage() {
            return (getPageCount() - 1) * countByPage;
        }

        function getPageCount() {
            var total = getTotalCount(),
                pageCount = parseInt(total / countByPage, 10)
                            + (total % countByPage === 0 ? 0 : 1);
            log.debug('getPageCount, pageCount=' + pageCount);
            return pageCount;
        }

        function isLastPage() {
            return getPageCount() === getCurrentPage();
        }

        function isItemFull() {
            return getTotalCount() % countByPage === 0;
        }

        function goBlue() {
            log.debug('goBlue');
            var shortcutObj = position[FOCUS_NAME_SHOTCUT]; 
            if (shortcutObj) {
                shortcutObj['action'](shortcutObj['param']);
            }
        }

        function hasButtonFocus() {
            return currentFocusName === FOCUS_NAME_CLOSE
                || currentFocusName === FOCUS_NAME_ACTION;
        }

        function hasContentFocus() {
            return currentFocusName === FOCUS_NAME_CONTENT;
        }

        function moveFocusButton() {
            changeFocus(currentFocusName, (currentFocusName === FOCUS_NAME_CLOSE ? FOCUS_NAME_ACTION : FOCUS_NAME_CLOSE));
        }

        function focusInContent() {
            changeFocus(currentFocusName, FOCUS_NAME_CONTENT);
        }

        function focusOut(nextButtonFocusName) {
            changeFocus(currentFocusName, nextButtonFocusName);
        }

        function focusOutInput(nextButtonFocusName) {
            var textArea = getElement(FOCUS_NAME_CONTENT);
            log.debug('focusOutInput, nextButtonFocusName=' + nextButtonFocusName);

            if (textArea.value !== '') {
                addSendParam(textArea);
            }
            focusOut(nextButtonFocusName);
        }

        function focusOutEmoticon(nextButtonFocusName) {
            focusOut(nextButtonFocusName);
            drawEmoticonList(true);
        }

        function addSendParam(element) {
            var focusObj = position[FOCUS_NAME_ACTION],
                param = {};

            param[KEY.SERVICE_CODE] = parameter[KEY.SERVICE_CODE];
            param[KEY.SAID] = parameter[KEY.SAID];
            log.debug('addSendParam, serviceCode, parameter=' + parameter[KEY.SERVICE_CODE]
                        + ', param=' + param[KEY.SERVICE_CODE]);
            param[KEY.PHONE] = parameter[KEY.PHONE];
            param[KEY.UUID] = parameter[KEY.UUID];

            if (isTextAreaTag(element)) {
                log.debug('addSendParam, element.value=' + element.value);
                param[KEY.REPLY_MESSAGE] = element.value;
            } else if (isCanvasTag(element)) {
                log.debug('addSendParam, focusIndex=' + focusIndex);
                param[KEY.EMOTICON_SELECTED_INDEX] = focusIndex;
                param[KEY.REPLY_MESSAGE] = emoticonArray[focusIndex]['desc'];
            }
            focusObj['param'] = param;

            checkMovableButton();
            setLastButton(FOCUS_NAME_ACTION);
        }

        function setLastButton(focusName) {
            log.debug('setLastButton, focusName=' + focusName);
            lastButtonFocusName = focusName;
        }

        function doAction() {
            var focusObj = position[currentFocusName],
                element;

            if (currentFocusName === FOCUS_NAME_CONTENT) {
                element = focusObj['element'];

                if (isTextAreaTag(element)) {
                    focusOutInput(lastButtonFocusName);
                } else if (isCanvasTag(element)) {
                    addSendParam(element);
                    focusOutEmoticon(FOCUS_NAME_ACTION);
                }
            } else if (focusObj) {
                log.debug('doAction, focusObj=' + focusObj);

                if (focusObj['param']) {
                    focusObj['action'](focusObj['param']);
                } else {
                    focusObj['action']();
                }
            }
        }

        function setFocus(focusID) {
            changeStyle(getElement(focusID), true);
            currentFocusName = focusID;
        }

        function changeFocus(unfocusID, focusID) {
            changeStyle(getElement(unfocusID), false);
            setFocus(focusID);
        }

        function changeStyle(element, hasFocus) {
            var currentClassName,
                className;

            if (element) {
                if (isTextAreaTag(element)) {
                    log.debug('changeStyle, textarea tag, hasFocus=' + hasFocus);
                    changeTextArea(hasFocus, element);
                } else if (isCanvasTag(element)) {
                    log.debug('changeStyle, canvas tag, hasFocus=' + hasFocus);
                } else {
                    log.debug('changeStyle, hasFocus=' + hasFocus);
                    currentClassName = $.getClass(element);

                    if (currentClassName) {
                        className = currentClassName.substring(0, currentClassName.lastIndexOf('_') + 1);
                        $.setAttribute(element, {
                            'class': className + (hasFocus ? 'focus' : 'unfocus')
                        });
                    }
                }
            } else {
                log.error('changeStyle, element is invalid!');
            }
        }

        function changeTextArea(hasFocus, element) {
            log.debug('changeTextArea, element=' + element + ', hasFocus='
                    + hasFocus);

            if (!element) {
                element = getElement(FOCUS_NAME_CONTENT);
                log.debug('changeTextArea, element=' + element);
            }

            element.blur();

            if (hasFocus) {
                log.debug('changeTextArea, call focus!');
                $.setDisabledOSK(element, false);
                element.focus();
            } else {
                $.setDisabledOSK(element, true);
            }
        }

        function useTextArea() {
            var element = getElement(FOCUS_NAME_CONTENT);
            if (element && isTextAreaTag(element)) {
                log.debug('useTextArea, setCurrentFocus and addInputEvent');
                setCurrentFocus(FOCUS_NAME_CONTENT);
                addInputEvent(true, element);
            }
        }

        function clearTextArea() {
            var element = getElement(FOCUS_NAME_CONTENT);
            if (element && isTextAreaTag(element)) {
                log.debug('clearTextArea, blur and removeInputEvent');
                element.blur();
                addInputEvent(false, element);
            } 
        }

        function clearLoadImage() {
            log.debug('clearLoadImage');
            for (var i = 0; i < emoticonImgArray.length; i++) {
                emoticonImgArray[i] = null;
            }
        }

        function drawAnEmoticonImage(url, row, column){
            var index = row * COLUMN_EMOTICON + column, 
                image = emoticonImgArray[index];
//            console.log('drawEmoticonImage, url=' + url + ', index=' + index + ', row=' + row + ', column=' + column);

            if (image) {
                drawImageAtCanvas(image, column * 67 + 1, row * 67 + 1);
            } else {
                image = new Image();
                image.src = url;
                image.onload = function() {
//                    console.log('drawImage, onload, url=' + url + ', row=' + row + ', column=' + column);
                    drawImageAtCanvas(image, column * 67 + 1, row * 67 + 1);
                };
                image.onerror = function() {
                    console.log('drawImage, onerror, url=' + url);
                };
                $.setAttribute(image, {
                    'class' : 'unfocus'
                });
                emoticonImgArray[index] = image;
            }
        }

        function drawFocusImage() {
            if (hasContentFocus()) {
                var visibleRow = getFocusRow(),
                    column = getFocusColumn();
//                log.debug('drawFocusImage, focusIndex=' + focusIndex + ', [' + visibleRow + ', ' + column + ']');
                drawImageAtCanvas(focusImage, column * 67, visibleRow * 67);
            }
        }

        function drawImageAtCanvas(image, xCoord, yCoord){
            if (context && image) {
//                log.debug('drawImageAtCanvas, xCoord=' + xCoord + ', yCoord=' + yCoord);
                context.drawImage(image, xCoord, yCoord);
            }
        }

        function getRow(index) {
            var startIndex = parseInt(index / countByPage) * countByPage;
            return parseInt((index - startIndex) / COLUMN_EMOTICON, 10);
        }

        function getColumn(index) {
            return index % COLUMN_EMOTICON;
        }

        function getFocusRow() {
            return getRow(focusIndex);
        }

        function getFocusColumn() {
            return getColumn(focusIndex);
        }

        function drawEmoticonList(unfocus) {
            log.debug('drawEmoticonList, focusIndex=' + focusIndex);
            if (!context) {
                context = canvasElement.getContext('2d');
            }

            var beginIndex = parseInt(focusIndex / countByPage, 10) * countByPage,
                lastIndex = -1,
                selectedIndex = -1;

            if (beginIndex + countByPage > getTotalCount()) {
                lastIndex = getTotalCount();
            } else {
                lastIndex = beginIndex + countByPage;
            }

            context.clearRect(0, 0, canvasElement.width, canvasElement.height);
            context.save();

            if (unfocus) {
                if (hasParam()) {
                    selectedIndex = getParam()[KEY.EMOTICON_SELECTED_INDEX];
                }

                if (beginIndex <= selectedIndex && selectedIndex < lastIndex) {
                    log.debug('drawEmoticonList, selectedIndex=' + selectedIndex
                            + ', draw ' + beginIndex + '->' + lastIndex + ' index');
                    context.globalAlpha = 1;
                    drawEmoticonImage(selectedIndex, selectedIndex + 1);
                    context.globalAlpha = 0.4;
                    drawEmoticonImage(beginIndex, selectedIndex);
                    drawEmoticonImage(selectedIndex, lastIndex);
                } else {
                    log.debug('drawEmoticonList, draw ' + beginIndex + '->'
                            + lastIndex + ' index');
                    context.globalAlpha = 0.4;
                    drawEmoticonImage(beginIndex, lastIndex);
                }
            } else {
                log.debug('drawEmoticonList, focusIndex=' + focusIndex
                        + ', draw ' + beginIndex + '->' + lastIndex + ' index');
                drawEmoticonImage(beginIndex, lastIndex);
                loadFocusImage();
            }
            context.restore();
        }

        function drawEmoticonImage(beginIndex, lastIndex) {
            for (var i = beginIndex; i < lastIndex; i++) {
                drawAnEmoticonImage(emoticonArray[i]['url'], getRow(i), getColumn(i));
            }
        }

        function loadFocusImage() {
            log.debug('loadFocusImage, focusIndex=' + focusIndex);
            if (focusImage) {
                drawFocusImage();
            } else {
                focusImage = new Image();
                focusImage.src = 'images/focus_emoticon.png';
                focusImage.onload = function(){
                    log.debug('loadFocusImage, onload, focusIndex=' + focusIndex);
                    drawFocusImage();
                };
                focusImage.onerror = function(){
                    log.debug('loadFocusImage, onerror, focusIndex=' + focusIndex);
                };
            }
        }

        function addInputEvent(add, element) {
            log.debug('addInputEvent, add=' + add + ', element=' + element);
            if (element) {
                if (add) {
                    element.addEventListener('input', inputIME);
                } else {
                    element.removeEventListener('input', inputIME);
                }
            }
        }

        return {
            FOCUS_NAME_CONTENT : FOCUS_NAME_CONTENT,
            FOCUS_NAME_CLOSE : FOCUS_NAME_CLOSE,
            FOCUS_NAME_ACTION : FOCUS_NAME_ACTION,
            FOCUS_NAME_SHOTCUT : FOCUS_NAME_SHOTCUT,
            add : add,
            setCurrentFocus : setCurrentFocus,
            hasContentFocus : hasContentFocus,
            setParam : setParam,
            goUp : goUp,
            goDown : goDown,
            goLeft : goLeft,
            goRight : goRight,
            goBlue : goBlue,
            backSpace : backSpace,
            doAction : doAction,
            drawEmoticonList : drawEmoticonList,
            manyPage : manyPage,
            setEmoticonUrl : setEmoticonUrl,
            useTextArea : useTextArea,
            clearTextArea : clearTextArea,
        };
    }

    Object.defineProperties(popup, {
        showPopup : {
            value : showPopup,
            writable : false
        },
        hidePopup : {
            value : hidePopup,
            writable : false
        },
        setKidsMethod : {
            value : setKidsMethod,
            writable : false,
        }
    });

    return popup;
});
