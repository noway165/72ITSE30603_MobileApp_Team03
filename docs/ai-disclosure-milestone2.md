# AI Usage Disclosure – Milestone 2

- **Tool used:** Claude (Claude Code)
- **What it did:** restructured the project into MVVM packages (`data/`, `feature/<screen>/ui`, `feature/<screen>/viewmodel`), wrote the ViewModels / repositories / ViewBinding screens, merged the duplicated `res/` folder into `app/src/main/res`, and found the runtime layout bug in `fragment_dashboard.xml` (Barrier referencing 0dp Views).
- **How we checked it:** built with `gradlew assembleDebug` + `lintDebug` (no errors), ran every screen on the Pixel 6 emulator, rotated the Add transaction screen to confirm state and validation errors survive, and grep-checked the five Milestone 2 rules (see `docs/MVVM_ARCHITECTURE.md`).
- **Team responsibility:** each member must be able to explain the ViewModel and Fragment of the screens they own (see `docs/UI_LAYOUTS.md`).
