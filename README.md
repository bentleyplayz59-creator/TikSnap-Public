# TikSnap

## Previous task: McDonald's and Windex posts. DONE

Great work. The McDonald's and Windex posts were updated correctly and the task is **successfully completed**. Nothing more needs to be done on the posts. Leave them as they are.

For reference, these are the finished images the posts use. Keep using them exactly as they are:

| File | Used for |
|---|---|
| `ad-assets/plain_image/Windex.jpg` | The picture in the Windex post |
| `ad-assets/plain_image/McDonalds.jpg` | The picture in the McDonald's post |
| `ad-assets/posters/Windex_header.jpg` | Top of the Windex post: avatar, name, verified badge |
| `ad-assets/posters/McDonalds_header.jpg` | Top of the McDonald's post: avatar, name, verified badge |

---

## NEW TASK: editable followers and following counts in Settings

### What to build

On the Profile screen, the stats card shows three numbers: **Posts**, **Followers**, and **Following** (currently 0, 1,284 and 7). Add a way to edit the Followers and Following numbers from inside the app's **Settings**.

### Requirements

1. **Settings option:** Add a section in Settings (for example "Profile stats") with two editable number fields:
   - **Followers**: how many followers the account has (default 1,284)
   - **Following**: how many people the account is following (default 7)
2. **Saves automatically:** When the user changes a number and saves, keep it stored so it is still there after the app is closed and reopened.
3. **Shows everywhere:** The new numbers must update on the Profile screen's stats card, and anywhere else in the app that shows a followers or following count. Do not leave any place showing the old hard-coded numbers.
4. **Built in properly:** Make this a real part of Settings, wired into the same place the other settings live. It should not be a separate screen or a quick hack.
5. **Number formatting:** Show large numbers with commas (for example 1,284), the same as the Profile screen does now.
6. **Valid input only:** Only allow whole numbers that are 0 or higher. If the field is empty or invalid, keep the previous value.
7. **Posts count:** Leave the Posts number as it is. It still counts the actual posts.

### Rules

- Do not change the McDonald's or Windex posts or any images in `ad-assets/`.
- Keep the existing look of the app (dark theme, pink/purple/orange accents).
- When finished, make sure the project still builds.
