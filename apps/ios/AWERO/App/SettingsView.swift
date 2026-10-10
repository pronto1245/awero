import SwiftUI


struct SettingsView: View {
    let onBack: () -> Void

    private var deviceLanguage: String {
        Locale.current.localizedString(forIdentifier: Bundle.main.preferredLocalizations.first ?? "en")
            ?? Bundle.main.preferredLocalizations.first ?? "en"
    }

    var body: some View {
        ZStack {
            AweroDesign.ivory.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    HStack {
                        Button(action: onBack) {
                            Image(systemName: "chevron.left")
                                .font(.headline.weight(.semibold))
                                .foregroundStyle(AweroDesign.navy)
                                .frame(width: 44, height: 44)
                                .background(AweroDesign.surface, in: Circle())
                        }
                        .accessibilityLabel(Text("settings.back"))
                        .accessibilityIdentifier("settings.back")
                        Spacer()
                        Text("AWERO")
                            .font(.system(size: 24, weight: .black))
                            .foregroundStyle(AweroDesign.navy)
                        Spacer()
                        Color.clear.frame(width: 44, height: 44)
                    }
                    .padding(.top, 8)

                    Text("settings.title")
                        .font(.largeTitle.bold())
                        .foregroundStyle(AweroDesign.navy)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("settings.title")

                    VStack(alignment: .leading, spacing: 8) {
                        Text("settings.language_title")
                            .font(.headline)
                            .foregroundStyle(AweroDesign.navy)
                        Text(deviceLanguage)
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(AweroDesign.navy)
                        Text("settings.language_body")
                            .font(.subheadline)
                            .foregroundStyle(AweroDesign.navy.opacity(0.68))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(AweroDesign.surface, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))

                    VStack(alignment: .leading, spacing: 10) {
                        Text("settings.alarm_permissions_title")
                            .font(.headline)
                            .foregroundStyle(AweroDesign.navy)
                        Text("settings.alarm_permissions_body")
                            .font(.subheadline)
                            .foregroundStyle(AweroDesign.navy.opacity(0.68))
                        Button {
                            guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                            UIApplication.shared.open(url)
                        } label: {
                            Text("settings.open_system_settings")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 13)
                                .background(AweroDesign.coral)
                                .foregroundStyle(.white)
                                .clipShape(RoundedRectangle(cornerRadius: AweroDesign.Corner.control))
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(16)
                    .background(AweroDesign.surface, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))
                }
                .padding(.horizontal, AweroDesign.Space.page)
                .padding(.bottom, 16)
            }
        }
    }
}
