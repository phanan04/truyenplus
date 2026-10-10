Real excerpts from dylan-byte-max/novel-tracker commit 80f3dfaf2cc4328758f0e24e8f4b7236d14dd735, fetched 2026-10-10. The main fixtures keep the first real book; qidian-new.json keeps one real new-on-ranking entry and up to 24 description characters, with original feed metadata. These are test excerpts, never app seed/demo data. Full live feeds are fetched by the app.

TruyenFull HTML fetched 2026-10-10 from public pages:

- `truyenfull-hot.html`: https://truyenfull.live/danh-sach/truyen-hot/ (original SHA-256 `1afefc353481cf9e59941c9a68f93c38edaf4545cc9a511b3dd05f8dec1304ee`).
- `truyenfull-new.html`: https://truyenfull.live/danh-sach/truyen-moi/ (original SHA-256 `2391e557bc71c99cc4cfd62391809cc02f581163a256a81513fccf576017c6bb`).
- `truyenfull-detail.html`: https://truyenfull.live/than-dao-dan-ton-6060282/ (original SHA-256 `949d50e1d7e6de2129e50879fa7c98809d50861d563d88ae9d5957ff6921b4bc`).
- `truyenfull-new-detail.html`: https://truyenfull.live/dai-ha-phe-vat-ky-ca-trieu-dinh-cung-ta-an-dua/ (original SHA-256 `4564f947fb5b88f38f85e525955964a8a8c6f837ae832d672b2727aa478e7824`).

HTML scripts and unnecessary page wrappers may be removed; catalog fields remain actual source data. `truyenfull.json` is one real book normalized by the production parser/fetcher from those downloaded pages, for the shared repository/cache tests. All fixtures are test-only. Live verification found 25 complete records out of 42 candidates: 20 hot and 5 recently updated, including 8 accessible daily ranking entries. The other 17 detail pages were inaccessible or redirected to search; their ranks/metadata were not invented.

## TruyenHH / TruyenYY (2026-10-10)

TruyenHH HTML tải từ các trang công khai dưới đây; fixture bỏ script và khoảng trắng cuối dòng. `truyenhh.json` trích metadata Book JSON-LD thật của trang chi tiết, hạng 1 đối chiếu bảng tuần.

- truyenhh-home.html: https://truyenhh.com/; original SHA256 `c659347d28199c24de78e540b1e913c200bf238deef0be8e5a52441580c5b877`.
- truyenhh-rank.html: https://truyenhh.com/bang-xep-hang/; original SHA256 `308850907ce979f572d7e6c39fc37a814fe8e36d1b6985a3fd26297de3eff8fa`.
- truyenhh-new.html: https://truyenhh.com/truyen/; original SHA256 `18ff5d1e5e5a592b62fe93f4e67bd6be93257224333496049d93f629d1336509`.
- truyenhh-detail.html: https://truyenhh.com/truyen/cung-chieu-co-vo-quan-nhan/; original SHA256 `92297e38abd6f4f2fe872962d95c8251b6d3c102c706f154f94cdd420a497156`.

`truyenyy.json` là trích đoạn tên/tác giả/giới thiệu/tag và URL Tu La Thiên Đế từ https://www.truyenyy.co/ được web reader trả ngày 2026-10-10; không có bìa hay hạng trong fixture này. HTTP trực tiếp đến TruyenYY không thành công trong môi trường local (DNS trả loopback). HTML nội tuyến của test YY là fixture cấu trúc có chủ đích, **không phải HTML capture thật**; không coi kết quả đó là xác minh hoạt động của website. Selector `.novel-info .name`, `.novel-info .author a`, `.novel-info img[data-src]` đối chiếu parser công khai https://github.com/dteviot/WebToEpub/blob/master/plugin/js/parsers/TruyenyyParser.js; không sao chép hay sử dụng chức năng lấy chương. Tất cả fixture chỉ nằm trong desktopTest, không đóng gói thành nguồn dữ liệu của app.
