'use strict';

acba.define("js:nav_util", ["log:kids(util)"], 
function(log) {
    log.level = log.DEBUG;

    var _NavUtil = {},
        NEW_LINE_CHAR = '\n',
        BR_TAG = '<br>';

    var stackTrace = function() {
        var e = new Error('dummy');
        var stack = e.stack.replace(/^[^\(]+?[\n$]/gm, '').replace(/^\s+at\s+/gm, '').replace(/^Object.<anonymous>\s*\(/gm, '{anonymous}()@').split('\n');
        
        log.debug(stack);
    };

    function id(id) {
        return document.getElementById(id);
    }

    function createElement(name) {
        return document.createElement(name);
    }

    function makeDiv(attrObj, value) {
        var element = createElement('div');
        if (element) {
            if (attrObj) {
                setAttribute(element, attrObj);
            }
            if (value) {
                element.innerHTML = value;
            }
        }
        return element;
    }

    function makeElement(name, attrObj, rootElement) {
        var anElement = createElement(name);
        if (anElement) {
            if (attrObj) {
                setAttribute(anElement, attrObj);
            }
            if (rootElement) {
                rootElement.appendChild(anElement);
            }
        } else {
            log.error('makeElement, createElement(' + name + ') is null');
        }
        return anElement;
    }

    function makeDivAppendChild(rootElement, attrObj, value) {
        var aDiv = makeDiv(attrObj, value);
        if (aDiv) {
            if (rootElement) {
                rootElement.appendChild(aDiv);
            } else {
                log.error('makeDivAppendChild, appendChild failed!, rootElement is null');
            }
        } else {
            log.error("makeDivAppendChild, createElement(" + attrObj + ") is null");
        }

        return aDiv;
    }

    function appendChild(rootElement, childElement) {
        if (rootElement) {
            if (childElement) {
                rootElement.appendChild(childElement);
            } else {
                 log.error('appendChild, childElement is null');
            }
        } else {
            log.error('appendChild, rootElement is null');
        }
    }

    function setAttribute(anElement, attrObj) {
        if (anElement) {
            for (var key in attrObj) {
                if (attrObj.hasOwnProperty(key)) {
//                    log.debug('setAttribute, key=' + key + ', value=' + attrObj[key]);
                    anElement.setAttribute(key, attrObj[key]);
                }
            }
        }
        return anElement;
    }

    function clearElement(anElement) {
        if (anElement) {
            anElement.innerHTML = "";
        }
    }

    function showElement(element) {
        if (element) {
            log.debug("showElement, element is " + element.id);
            element.style.display = 'block';
        }
        else {
            log.debug("showElement, element is null");
        }
    }

    function hideElement(element) {
        if (element) {
            log.debug("hideElement, element is " + element.id);
            element.style.display = 'none';
        }
        else {
            log.debug("hideElement, element is null");
        }
    }

    function isViewingElement(element) {
        if (element) {
            log.debug("isViewingElement, element=" + element.id + ", style.display=" + element.style.display);
            if (element.style.display == 'none') {
                return false;
            }
            else {
                return true;
            }
        }
        else {
            log.debug("isViewingElement, element is null");
        }
    }

    function getComputedStyle(element, propertyName) {
        var value = 0;

        if (element && propertyName) {
            try {
                value = parseInt(document.defaultView.getComputedStyle(element, null).getPropertyValue(propertyName));
            } catch (e) {
                log.error(e);
            }
        }
        return value;
    }

    function textIndent(element) {
        return getComputedStyle(element, "text-indent");
    }

    function left(element) {
        return getComputedStyle(element, "left");
    }

    function width(element) {
        return getComputedStyle(element, "width");
    }

    function height(element) {
        return getComputedStyle(element, "height");
    }

    function bottom(element) {
        return getComputedStyle(element, "bottom");
    }

    function getClass(element) {
        if (element) {
            return element.getAttribute('class');
        } else {
            log.debug("getClass, element is null");
        }
    }

    function lineHeight(element) {
        return getComputedStyle(element, "line-height");
    }

    function replaceToBR(_message) {
        var i = 0, findIndex = 0, message = '';

        while ((findIndex = _message.indexOf(NEW_LINE_CHAR, i)) !== -1) {
            message += (_message.substring(i, findIndex) + BR_TAG);
            i = findIndex + 1;
        }
        message += _message.substring(i);

        //        log.debug('replaceToBR, message=' + message);

        return message;
    }

    function checkElement(element, from) {
        if (element) {
            return true;
        } else {
            log.debug('checkElement in the ' + from + ', element error, element is \'' + element + '\'');
            return false;
        }
    }

    function setDisabledOSK(element, add) {
        if (checkElement(element, 'setDisabledOSK')) {
            var has = element.hasAttribute('disabled');
            if (add ^ has) {
                if (has) {
                    element.removeAttribute('disabled');
                } else {
                    element.blur();
                    element.setAttribute('disabled', '');
                }
            }
        }
    };

    Object.defineProperties(_NavUtil, {
        stackTrace: {
            value: stackTrace,
            writable: false
        },
        id: {
            value: id,
            writable: false
        },
        makeElement: {
            value: makeElement,
            writable: false
        },
        makeDiv: {
            value: makeDiv,
            writable: false
        },
        makeDivAppendChild: {
            value: makeDivAppendChild,
            writable: false
        },
        appendChild : {
           value : appendChild,
           writable: false 
        },
        setAttribute: {
            value: setAttribute,
            writable: false
        },
        textIndent: {
            value: textIndent,
            writable: false
        },
        left: {
            value: left,
            writable: false
        },
        width: {
            value: width,
            writable: false
        },
        height: {
            value: height,
            writable: false
        },
        bottom: {
           value: bottom,
           writable: false 
        },
        lineHeight: {
            value : lineHeight,
            writable: false
        },
        getClass: {
            value : getClass,
            writable: false
        },
        clearElement: {
            value: clearElement,
            writable: false
        },
        showElement: {
            value: showElement,
            writable: false
        },
        hideElement: {
            value: hideElement,
            writable: false
        },
        isViewingElement: {
            value: isViewingElement,
            writable: false
        },
        replaceToBR: {
            value: replaceToBR,
            writable: false
        },
        setDisabledOSK : {
            value : setDisabledOSK,
            writable: false
        }
    });

    return {
        NavUtil: _NavUtil
    };
}); 
