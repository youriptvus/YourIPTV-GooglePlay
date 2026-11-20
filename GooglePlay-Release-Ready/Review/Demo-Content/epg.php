<?php
header('Content-Type: application/xml; charset=utf-8');
header('Cache-Control: public, max-age=300');
header('X-Content-Type-Options: nosniff');
$now = time(); $start = intdiv($now, 3600) * 3600 - 86400;
echo '<?xml version="1.0" encoding="UTF-8"?>' . "\n";
echo '<tv generator-info-name="YourIPTV authorized generated review guide">';
for ($channel = 1; $channel <= 2; $channel++) {
  echo '<channel id="synthetic' . $channel . '"><display-name>Synthetic Channel ' . $channel . '</display-name></channel>';
}
for ($channel = 1; $channel <= 2; $channel++) {
  for ($hour = 0; $hour < 192; $hour++) {
    $a = $start + $hour * 3600; $b = $a + 3600;
    echo '<programme start="' . gmdate('YmdHis', $a) . ' +0000" stop="' . gmdate('YmdHis', $b) . ' +0000" channel="synthetic' . $channel . '">';
    echo '<title>Authorized generated test pattern</title><desc>Locally generated pattern and sine audio. No television or subscription content.</desc></programme>';
  }
}
echo '</tv>';
