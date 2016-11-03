#!/bin/sh

e3db=./e3db-0.5.0/bin/e3db
iID=166ed61a-3a56-4fe6-980f-2850aa82ea25

echo "------------------------------------------------------------"
echo "-- Welcome to the E3DB quick feedback script."
echo "------------------------------------------------------------"
echo "This script gets your input, writes it to E3DB, and shares it with us."
echo "All your feedback will be end-to-end encrypted with your key and ours."
echo "Since E3DB is experimental, you may want to git pull the latest version first.\n"
echo "What are your initial impressions?"
read impressions
feedback="$e3db write feedback '{\"feedback\":\"$impressions\"}'"
echo "We're about to send this. $feedback"
echo "OK to send? [y/n] "
read okToSend
if [ $okToSend == 'y' ]; then
    echo "Writing to E3DB..."
    share=`$e3db write feedback {\"feedback\":\"$impressions\"}`
    echo "Record ID: $share"
    echo "Sharing with Tozny..."
    $e3db share feedback $iID
    echo "Here's the shared record:"
    $e3db read $share
    echo "Thanks! Please keep trying it out and sending more feedback!"
else
    echo "OK, re-run when you're happy".
fi
