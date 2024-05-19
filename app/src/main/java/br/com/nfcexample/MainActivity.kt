package br.com.nfcexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import br.com.nfcexample.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // tenta obter o adaptador NFC, se nao for suportado pleo dispositivo, exibe mensagem e encerra a atividade
        nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: let {
            Toast.makeText(this, "NFC não suportado neste dispositivo", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //Configura o PendingIntent para que a aplicação seja reaberta quando uma tag NFC é detectada.
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        // IntentFilter: Configura o filtro para detectar mensagens NDEF.
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Falha ao adicionar tipo MIME.", e)
            }
        }
        intentFilters = arrayOf(ndef)

        binding.readNfcButton.setOnClickListener {
            enableNfcReaderMode()
        }
    }
    // Ativa a captura de intents NFC. Usamos nfcAdapter? para evitar NullPointerException.
    private fun enableNfcReaderMode() {
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        Toast.makeText(this, "Aproxime uma tag NFC", Toast.LENGTH_SHORT).show()
    }

    //Este método é chamado quando um novo intent é recebido. Verifica se o intent é de uma tag NDEF e, em caso afirmativo, lê a tag.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                readFromTag(it)
            }
        }
    }
    //Conecta à tag NFC, lê a mensagem NDEF, converte os dados para string e exibe no TextView.
    private fun readFromTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        ndef?.connect()
        val ndefMessage = ndef?.ndefMessage
        val message = ndefMessage?.records?.joinToString("\n") { record ->
            String(record.payload)
        }
        binding.nfcContent.text = message ?: "Nenhum dado encontrado"
        ndef?.close()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
}
