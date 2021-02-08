# IF2211-STIMA-GREEDY

Kahfi Soobhan Zulkifli 13519012 <br />
Jose Galbraith Hasintongan 13519022 <br />
Ryandito Diandaru 13519157

Himpunan Kandidat: 
Seluruh cacing yang aktif

Himpunan Solusi:
Cacing musuh

Fungsi Solusi:
Memeriksa apakah cacing yang dipilih itu cacing musuh

Fungsi Seleksi: <br />
- Tergantung role cacing <br />
  - Komando: <br />
    Select Technologist buat nyerang/mendekat ke musuh. <br />
    Jika Techno keos, nyuruh agent buat nyerang/mendekat. <br />
    Jika dua2nya keos, komando move nyamping random atau kalau dekat musuh, tembak. <br />
  - Agent: <br />
    Jika HP tinggi, mendekat/melempar banana bomb ke musuh <br />
    Jika HP rendah, Select komando buat mendekat/shoot <br />
  - Technologist: <br />
    Jika HP tinggi, mendekat/melempar snowball <br />
    Jika HP rendah, select agent buat move/banana bomb. <br />
    Jika Agent keos, select commando buat nmove/shoot. <br />

Fungsi Kelayakan:
Cacing yang dipilih HPnya rendah atau jaraknya dekat

Fungsi Obyektif:
Meminimumkan jumlah langkah-langkah untuk membunuh semua cacing
