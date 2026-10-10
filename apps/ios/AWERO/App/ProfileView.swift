import SwiftUI

struct ProfileView: View {
    let onOpenSettings: () -> Void

    var body: some View {
        ZStack {
            AweroDesign.ivory.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text("AWERO")
                        .font(.system(size: 24, weight: .black))
                        .foregroundStyle(AweroDesign.navy)
                    Text("nav.profile")
                        .font(.largeTitle.bold())
                        .foregroundStyle(AweroDesign.navy)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityIdentifier("nav.profile")
                    Button(action: onOpenSettings) {
                        HStack(spacing: 14) {
                            Image(systemName: "gearshape")
                                .font(.system(size: 22, weight: .semibold))
                                .foregroundStyle(AweroDesign.coral)
                                .frame(width: 44, height: 44)
                            VStack(alignment: .leading, spacing: 3) {
                                Text("settings.title")
                                    .font(.headline)
                                    .foregroundStyle(AweroDesign.navy)
                                Text("profile.settings_hint")
                                    .font(.subheadline)
                                    .foregroundStyle(AweroDesign.navy.opacity(0.68))
                                    .multilineTextAlignment(.leading)
                            }
                            Spacer(minLength: 8)
                            Image(systemName: "chevron.right")
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(AweroDesign.coral)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(14)
                        .background(AweroDesign.surface, in: RoundedRectangle(cornerRadius: AweroDesign.Corner.card))
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("profile.settings")
                }
                .padding(.horizontal, 20)
                .padding(.top, 18)
                .padding(.bottom, 24)
            }
        }
    }
}
