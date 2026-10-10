Real excerpts from dylan-byte-max/novel-tracker commit 80f3dfaf2cc4328758f0e24e8f4b7236d14dd735, fetched 2026-10-10. The main fixtures keep the first real book; qidian-new.json keeps one real new-on-ranking entry and up to 24 description characters, with original feed metadata. These are test excerpts, never app seed/demo data. Full live feeds are fetched by the app.

TruyenFull HTML fetched 2026-10-10 from public pages:

- `truyenfull-hot.html`: https://truyenfull.live/danh-sach/truyen-hot/ (original SHA-256 `1afefc353481cf9e59941c9a68f93c38edaf4545cc9a511b3dd05f8dec1304ee`).
- `truyenfull-new.html`: https://truyenfull.live/danh-sach/truyen-moi/ (original SHA-256 `2391e557bc71c99cc4cfd62391809cc02f581163a256a81513fccf576017c6bb`).
- `truyenfull-detail.html`: https://truyenfull.live/than-dao-dan-ton-6060282/ (original SHA-256 `949d50e1d7e6de2129e50879fa7c98809d50861d563d88ae9d5957ff6921b4bc`).
- `truyenfull-new-detail.html`: https://truyenfull.live/dai-ha-phe-vat-ky-ca-trieu-dinh-cung-ta-an-dua/ (original SHA-256 `4564f947fb5b88f38f85e525955964a8a8c6f837ae832d672b2727aa478e7824`).

HTML scripts and unnecessary page wrappers may be removed; catalog fields remain actual source data. `truyenfull.json` is one real book normalized by the production parser/fetcher from those downloaded pages, for the shared repository/cache tests. All fixtures are test-only. Live verification found 25 complete records out of 42 candidates: 20 hot and 5 recently updated, including 8 accessible daily ranking entries. The other 17 detail pages were inaccessible or redirected to search; their ranks/metadata were not invented.
