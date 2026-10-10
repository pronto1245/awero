import SwiftUI


struct SettingsView: View {
    private var deviceLanguage: String {
        Locale.current.localizedString(forIdentifier: Bundle.main.preferredLocalizations.first ?? "en")
            ?? Bundle.main.preferredLocalizations.first ?? "en"
    }

    var body: some View {
        ZStack {
            AweroDesign.ivory.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
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
                    .padding(20)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    VStack(alignment: .leading, spacing: 12) {
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
                                .padding(.vertical, 14)
                                .background(AweroDesign.coral)
                                .foregroundStyle(AweroDesign.navy)
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(20)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                }
                .padding(20)
            }
        }
        .navigationTitle("settings.title")
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(AweroDesign.ivory, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }
}
