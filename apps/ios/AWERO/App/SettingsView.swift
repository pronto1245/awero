import SwiftUI

private enum SettingsStyle {
    static let ivory = Color(red: 1.0, green: 0.973, blue: 0.937)
    static let navy = Color(red: 0.078, green: 0.161, blue: 0.294)
    static let coral = Color(red: 1.0, green: 0.408, blue: 0.294)
}

struct SettingsView: View {
    private var deviceLanguage: String {
        Locale.current.localizedString(forIdentifier: Locale.current.identifier)
            ?? Locale.current.identifier
    }

    var body: some View {
        ZStack {
            SettingsStyle.ivory.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("settings.language_title")
                            .font(.headline)
                            .foregroundStyle(SettingsStyle.navy)
                        Text(deviceLanguage)
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(SettingsStyle.navy)
                        Text("settings.language_body")
                            .font(.subheadline)
                            .foregroundStyle(SettingsStyle.navy.opacity(0.68))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(20)
                    .background(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 20))

                    VStack(alignment: .leading, spacing: 12) {
                        Text("settings.alarm_permissions_title")
                            .font(.headline)
                            .foregroundStyle(SettingsStyle.navy)
                        Text("settings.alarm_permissions_body")
                            .font(.subheadline)
                            .foregroundStyle(SettingsStyle.navy.opacity(0.68))
                        Button {
                            guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                            UIApplication.shared.open(url)
                        } label: {
                            Text("settings.open_system_settings")
                                .font(.headline)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(SettingsStyle.coral)
                                .foregroundStyle(.white)
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
        .toolbarBackground(SettingsStyle.ivory, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
    }
}
