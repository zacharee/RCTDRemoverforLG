#!/system/bin/sh

REM_RCTD=$1
REM_CCMD=$2
REM_TRITON=$3
DEVICE_CODE=$4

COLOR_GRN_PRE="<font color='#00ff00'>"
COLOR_YEL_PRE="<font color='#ffff00'>"
COLOR_POS="</font>"

YELLOW=0
GREEN=1

echoAndExec() {
    COLOR=$1
    shift

    if [ "$COLOR" = ${YELLOW} ]; then
        echo "$COLOR_YEL_PRE $* $COLOR_POS"
    elif [ "$COLOR" = ${GREEN} ]; then
        echo "$COLOR_GRN_PRE $* $COLOR_POS"
    fi

    "${@}" || exit 1
}

echo "$COLOR_GRN_PRE Copying boot.img from device... $COLOR_POS"
echoAndExec ${YELLOW} dd if=/dev/block/bootdevice/by-name/boot of=/data/local/AIK-mobile/boot.img

echo "$COLOR_GRN_PRE Extracting boot.img... $COLOR_POS"
echoAndExec ${YELLOW} ./unpackimg.sh boot.img

echoAndExec ${YELLOW} cd ramdisk

if [ "$REM_RCTD" = "true" ]
then
    echo "$COLOR_GRN_PRE Removing RCTD... $COLOR_POS"
    echoAndExec ${YELLOW} sed -ir -e '\_^service rctd /sbin/rctd$_,/^$/{/^(#\|$)/!s/^/#/}' init.lge.rc
fi

if [ "$REM_CCMD" = "true" ]
then
    echo "$COLOR_GRN_PRE Removing CCMD... $COLOR_POS"
    echoAndExec ${YELLOW} sed -ir -e '\_^service ccmd /system/bin/ccmd$_,/^$/{/^(#\|$)/!s/^/#/}' init.lge.rc
fi

if [ "$REM_TRITON" = "true" ]
then
    echo "$COLOR_GRN_PRE Removing Triton... $COLOR_POS"
    echoAndExec ${YELLOW} sed -ir -e '/# triton service/,\_chmod 644 /sys/devices/system/cpu/triton/enable_s/^/# /' init.${DEVICE_CODE}.power.rc
fi

echoAndExec ${YELLOW} cd ../

echo "$COLOR_GRN_PRE Repacking boot.img... $COLOR_POS"
echoAndExec ${YELLOW} ./repackimg.sh

echo "$COLOR_GRN_PRE Moving modified boot.img to /sdcard/AndroidImageKitchen/boot.img... $COLOR_POS"
echoAndExec ${YELLOW} mv image-new.img /storage/emulated/0/AndroidImageKitchen/boot.img

echo "$COLOR_GRN_PRE Cleaning up... $COLOR_POS"
#echoAndExec ${YELLOW} ./cleanup.sh

echo "Done!"
