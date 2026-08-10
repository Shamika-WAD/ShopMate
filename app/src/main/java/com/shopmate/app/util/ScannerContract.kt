package com.shopmate.app.util

/**
 * Contract used to pass the scanned barcode value back from
 * ScannerFragment to whichever screen opened it (billing or product form),
 * via the Fragment Result API.
 */
object ScannerContract {
    const val REQUEST_KEY = "scan_result"
    const val KEY_BARCODE = "barcode"
}
