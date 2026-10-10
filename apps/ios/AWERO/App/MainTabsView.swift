import SwiftUI

struct AweroMainTabsView: View {
    @Binding var showingCreate: Bool
    @State private var selectedTab = 0
    @ScaledMetric(relativeTo: .caption) private var tabLabelSize: CGFloat = 11

    var body: some View {
        VStack(spacing: 0) {
            Group {
                switch selectedTab {
                case 1:
                    AweroProgressView(onSetAlarm: {
                        selectedTab = 0
                        showingCreate = true
                    }, onOpenSettings: {
                        selectedTab = 3
                    })
                case 2:
                    ProfileView(onOpenSettings: { selectedTab = 3 })
                case 3:
                    SettingsView(onBack: { selectedTab = 2 })
                default:
                    HomeView(showingCreate: $showingCreate, onOpenSettings: { selectedTab = 3 })
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            HStack(spacing: 0) {
                tab(title: "nav.home", icon: "house.fill", index: 0)
                tab(title: "nav.progress", icon: "chart.bar.fill", index: 1)
                tab(title: "nav.profile", icon: "person.fill", index: 2, selected: selectedTab >= 2)
            }
            .padding(.horizontal, 14)
            .padding(.top, 7)
            .padding(.bottom, 4)
            .background(AweroDesign.ivory)
            .overlay(alignment: .top) {
                Rectangle().fill(AweroDesign.navy.opacity(0.07)).frame(height: 1)
            }
        }
        .background(AweroDesign.ivory.ignoresSafeArea())
        .tint(AweroDesign.coral)
    }

    private func tab(title: LocalizedStringKey, icon: String, index: Int, selected: Bool? = nil) -> some View {
        Button { selectedTab = index } label: {
            VStack(spacing: 3) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .semibold))
                    .frame(height: 23)
                Text(title)
                    .font(.system(size: tabLabelSize, weight: .medium))
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
            }
            .foregroundStyle((selected ?? (selectedTab == index)) ? AweroDesign.coral : AweroDesign.navy.opacity(0.58))
            .frame(maxWidth: .infinity, minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("tab.\(index)")
        .accessibilityAddTraits((selected ?? (selectedTab == index)) ? .isSelected : [])
    }
}
